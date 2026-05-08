package it.polimi.ingsw.am02.client.network.socket;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ClientNetworkDispatcher;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.PingEvent;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Client-side proxy for Socket connection to the server.
 * Handles command sending and asynchronous event reception.
 */
public class SocketServerProxy implements ServerProxy {

    private static final long PING_INTERVAL_MILLIS = 5_000;
    private static final long PING_TIMEOUT_MILLIS  = 10_000;

    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView view;
    private final JsonMessageCodec codec = new JsonMessageCodecImpl();

    private ClientController clientController;
    private ClientNetworkDispatcher dispatcher; // Unified event receiver

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;

    private Thread pingThread;
    private volatile long lastPongReceivedAt = 0;

    // Data stored to support reconnection logic
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
        this.dispatcher = new ClientNetworkDispatcher(controller, lobbyModel);
        this.dispatcher.setOnGameStarted(id -> this.activeGameId = id);
    }

    @Override
    public void connect() throws Exception {
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
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

    /**
     * Read loop: receives JSON strings, decodes them and delegates execution.
     */
    private void listenForEvents() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                lastPongReceivedAt = System.currentTimeMillis();
                Message msg = codec.decode(line);

                // Heartbeat handling (protocol level)
                if (msg instanceof PingEvent) {
                    send(new PongCommand());
                    continue;
                }

                // Inversion of Control: the event knows which dispatcher method to call
                if (msg instanceof Event event) {
                    event.apply(dispatcher);
                }
            }
        } catch (IOException e) {
            if (connected) {
                handleConnectionLost();
            }
        } finally {
            connected = false;
        }
    }

    private synchronized void handleConnectionLost() {
        if (!connected) return;
        connected = false;
        stopPingThread();
        view.onConnectionLost();

        new Thread(() -> {
            while (!connected) {
                try {
                    Thread.sleep(5_000);
                    try { if (socket != null) socket.close(); } catch (IOException ignored) {}
                    connect();
                    if (activeNickname != null && activeGameId != null) {
                        requestReconnect(activeNickname, activeGameId);
                    } else if (activeNickname != null) {
                        requestSetUsername(activeNickname);
                        view.onReturnToLobby();
                    }
                    view.onConnectionRestored();
                } catch (Exception ignored) {}
            }
        }, "SocketProxy-ReconnectThread").start();
    }

    private void send(Command command) {
        if (out != null) {
            out.println(codec.encode(command));
            out.flush();
        }
    }

    // --- VirtualServer command implementation ---

    @Override
    public void requestSetUsername(String username) {
        this.activeNickname = username;
        send(new SetUsernameCommand(username));
    }

    @Override
    public void requestCreateLobby(int numPlayers) {
        send(new CreateLobbyCommand(numPlayers));
    }

    @Override
    public void requestJoinLobby(String lobbyID) {
        send(new JoinLobbyCommand(lobbyID));
    }

    @Override
    public void requestSelectTotem(Totem color) {
        send(new SelectTotemCommand(color));
    }

    @Override
    public void requestLeaveLobby() {
        send(new LeaveLobbyCommand(activeNickname != null ? activeNickname : ""));
    }

    @Override
    public void requestReconnect(String nickname, String gameId) {
        this.activeNickname = nickname;
        this.activeGameId = gameId;

        // FONDAMENTALE: Dobbiamo dire al dispatcher chi siamo.
        // Senza questo, il dispatcher riceverà i notify dal server ma
        // activeNickname sarà null, quindi non creerà mai il GameModel.
        if (this.dispatcher != null) {
            this.dispatcher.updateActiveNickname(nickname);
            this.dispatcher.updateActiveGameId(gameId);
        }

        send(new ReconnectCommand(nickname, gameId));
    }

    @Override
    public void moveTotem(char tileID) {
        send(new MoveTotemCommand("", tileID));
    }

    @Override
    public void resolveActions(List<String> ids) {
        send(new ResolveActionsCommand("", ids));
    }

    // --- Heartbeat logic ---

    private void startPingThread() {
        stopPingThread();
        pingThread = new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(PING_INTERVAL_MILLIS);
                    if (!connected) break;
                    long elapsed = System.currentTimeMillis() - lastPongReceivedAt;
                    if (elapsed > PING_TIMEOUT_MILLIS) {
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
}