package it.polimi.ingsw.am02.client.network.socket;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.error.ErrorEvent;
import it.polimi.ingsw.am02.common.messages.events.game.GameEvent;
import it.polimi.ingsw.am02.common.messages.events.game.PingEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.GameStartedEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.LobbyEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;
import it.polimi.ingsw.am02.common.messages.events.game.PingEvent;
import it.polimi.ingsw.am02.common.messages.commands.PongCommand;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Client-side proxy per la connessione Socket al server.
 * <p>
 * Speculare a {@link it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy}:
 * <ul>
 *   <li>Thread reader: legge eventi dal server e li smista tramite {@link ClientController}.</li>
 *   <li>Thread ping: invia periodicamente un {@link PingEvent} al server;
 *       se il server non risponde entro il timeout, segnala la connessione persa.</li>
 * </ul>
 * </p>
 */
public class SocketServerProxy implements ServerProxy {

    // --- Configurazione ping client→server ---
    private static final long PING_INTERVAL_MILLIS = 5_000;
    private static final long PING_TIMEOUT_MILLIS  = 10_000;

    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView view;

    private final JsonMessageCodec codec = new JsonMessageCodecImpl();

    private ClientController clientController;

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;

    // Ping state
    private Thread pingThread;
    private volatile long lastPongReceivedAt = 0;

    // Dati per eventuale riconnessione/routing
    private String activeNickname;
    private String activeGameId;

    public SocketServerProxy(String host, int port, LobbyModel lobbyModel, ClientView view) {
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.view = view;
    }

    @Override
    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

    @Override
    public void connect() throws Exception {
        socket = new Socket(host, port);
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
        );
        connected = true;
        lastPongReceivedAt = System.currentTimeMillis();

        Thread readerThread = new Thread(this::listenForEvents, "SocketProxy-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        startPingThread();
    }

    @Override
    public void disconnect() {
        connected = false;
        stopPingThread();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public boolean isConnected() { return connected; }

    // Reader: eventi in arrivo dal server
    private void listenForEvents() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                lastPongReceivedAt = System.currentTimeMillis();
                Message msg = codec.decode(line);
                if (msg instanceof PingEvent) {
                    out.println(codec.encode(new PongCommand()));
                    out.flush();
                    continue; // Ignora il resto del ciclo e aspetta il prossimo messaggio
                }
                if (msg instanceof Event event) {
                    route(event);
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("[SocketServerProxy] Connessione persa.");
                handleConnectionLost();
            }
        } finally {
            connected = false;
        }
    }

    /**
     * Smista l'evento ricevuto attraverso il {@link ClientController},
     * specularmente a quanto fa {@link it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy#notifyEvent}.
     */
    private void route(Event event) {
        // Aggiorna cache per riconnessione
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.activeNickname = e.username();
        }
        if (event instanceof GameStartedEvent e) {
            this.activeGameId = e.gameID();
        }
        if (event instanceof LobbyEvent lobbyEvent) {
            lobbyModel.apply(lobbyEvent);

            if (clientController.getGameModel() == null && activeNickname != null && activeGameId != null) {
                clientController.onGameModelRequired(activeNickname);
                clientController.getGameModel().setGameId(activeGameId);
            }
            if (clientController.getGameModel() != null) {
                clientController.getGameModel().apply(lobbyEvent);
            }

        } else if (event instanceof GameEvent gameEvent) {
            if (clientController.getGameModel() == null) {
                if (activeNickname == null) return;
                clientController.onGameModelRequired(activeNickname);
                if (activeGameId != null) {
                    clientController.getGameModel().setGameId(activeGameId);
                }
            }
            clientController.getGameModel().apply(gameEvent);

        } else if (event instanceof ErrorEvent errorEvent) {
            if (clientController.getGameModel() == null) {
                this.activeGameId = null;
            }
            if (clientController.getGameModel() != null) {
                clientController.getGameModel().apply(errorEvent);
            } else {
                lobbyModel.apply(errorEvent);
            }
        }
    }

    // Ping thread: client → server (specular to RmiServerProxy)
    /**
     * Invia periodicamente un {@link PingEvent} al server per verificare che sia ancora vivo.
     * Se non riceviamo un pong entro {@code PING_TIMEOUT_MILLIS}, la connessione è considerata persa.
     * <p>
     * Nota: nel protocollo Socket il "pong" del client verso il server avviene in risposta al
     * PingEvent del server (già gestito nel reader). Questo thread fa il ping nella direzione
     * opposta (client → server) riutilizzando lo stesso meccanismo: inviamo un PingEvent e
     * aspettiamo che il server risponda con PongCommand. Poiché però il server attuale non
     * gestisce un PingEvent in arrivo dal client, adottiamo una strategia più semplice e robusta:
     * verifichiamo che il server continui a mandarci i suoi PingEvent entro il timeout.
     * Se il timestamp dell'ultimo pong supera il timeout, segnaliamo la connessione persa.
     * </p>
     */
    private void startPingThread() {
        stopPingThread();
        pingThread = new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(PING_INTERVAL_MILLIS);
                    if (!connected) break;

                    long elapsed = System.currentTimeMillis() - lastPongReceivedAt;
                    if (elapsed > PING_TIMEOUT_MILLIS) {
                        System.out.println("[SocketServerProxy] Timeout: nessun segnale dal server da " + elapsed + "ms.");
                        handleConnectionLost();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "SocketProxy-PingThread");
        pingThread.setDaemon(true);
        pingThread.start();
    }

    private void stopPingThread() {
        if (pingThread != null) {
            pingThread.interrupt();
            pingThread = null;
        }
    }

    private void handleConnectionLost() {
        if (!connected) return;
        connected = false;
        stopPingThread();
        view.onConnectionLost();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // Invio comandi al server
    private void send(Command command) {
        if (out != null){
            out.println(codec.encode(command));
            out.flush();
        }
    }

    @Override public void requestSetUsername(String username)        { send(new SetUsernameCommand(username)); }
    @Override public void requestCreateLobby(int numPlayers)        { send(new CreateLobbyCommand(numPlayers)); }
    @Override public void requestJoinLobby(String lobbyID)          { send(new JoinLobbyCommand(lobbyID)); }
    @Override public void requestSelectTotem(Totem color)           { send(new SelectTotemCommand(color)); }
    @Override public void requestStartGame()                        { send(new StartGameCommand()); }
    @Override public void requestLeaveLobby()                       { send(new LeaveLobbyCommand(activeNickname != null ? activeNickname : "")); }
    @Override public void requestReconnect(String nickname, String gameId) {
        this.activeNickname = nickname;
        this.activeGameId   = gameId;
        send(new ReconnectCommand(nickname, gameId));
    }
    @Override public void moveTotem(char tileID)                    { send(new MoveTotemCommand("", tileID)); }
    @Override public void resolveActions(List<String> ids)          { send(new ResolveActionsCommand("", ids)); }
}
