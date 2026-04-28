package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.GameEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.GameStartedEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.LobbyEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;
import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerFactory;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClientRemote {
    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView clientView;
    private GameModel gameModel;
    private RmiServerRemote serverStub;

    private volatile boolean connected = false;
    private volatile boolean attemptingReconnection = false;

    // Cache vitale per l'Auto-Reconnect
    private String activeNickname = null;
    private String activeGameId = null;

    public RmiServerProxy(String host, int port, LobbyModel lobbyModel, ClientView clientView) throws RemoteException {
        super();
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.clientView = clientView;
    }

    @Override
    public void connect() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        RmiServerFactory factory = (RmiServerFactory) registry.lookup("AM02-GameServer");
        this.serverStub = factory.registerClient(this);
        this.connected = true;
    }

    @Override
    public void disconnect() {
        this.connected = false;
        try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) {}
    }

    @Override public boolean isConnected() { return connected; }
    @Override public void ping() throws RemoteException { /* Heartbeat dal server */ }

    // --- INBOUND ---
    @Override
    public void notifyEvent(Event event) throws RemoteException {
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.activeNickname = e.username();
        }
        if (event instanceof GameStartedEvent e) {
            this.activeGameId = e.gameID();
        }

        if (event instanceof LobbyEvent lobbyEvent) {
            lobbyModel.apply(lobbyEvent);
            if (event instanceof GameStartedEvent && gameModel == null) {
                initGameModel(this.activeNickname);
            }
        } else if (event instanceof GameEvent gameEvent) {
            if (gameModel == null) {
                initGameModel(this.activeNickname);
            }
            gameModel.apply(gameEvent);
        }
    }

    private void initGameModel(String nickname) {
        this.gameModel = new GameModel(nickname);
        if (clientView instanceof TuiView tui) {
            tui.onGameModelCreated(this.gameModel);
        }
        this.clientView.setGameModel(this.gameModel);
        if (this.activeGameId != null) {
            this.gameModel.apply(new GameStartedEvent(this.activeGameId));
        }
    }

    // --- OUTBOUND CON GESTIONE ERRORI RETE ---

    private void execute(NetworkAction action) {
        try {
            action.run();
        } catch (RemoteException e) {
            handleNetworkFailure(e);
        }
    }

    @FunctionalInterface interface NetworkAction { void run() throws RemoteException; }

    @Override public void requestSetUsername(String u) { execute(() -> serverStub.requestSetUsername(u)); }
    @Override public void requestCreateLobby(int n) { execute(() -> serverStub.requestCreateLobby(n)); }
    @Override public void requestJoinLobby(String id) { execute(() -> serverStub.requestJoinLobby(id)); }
    @Override public void requestSelectTotem(Totem t) { execute(() -> serverStub.requestSelectTotem(t)); }
    @Override public void requestStartGame() { execute(() -> serverStub.requestStartGame()); }
    @Override public void requestLeaveLobby() { execute(() -> serverStub.requestLeaveLobby()); }
    @Override public void moveTotem(char t) { execute(() -> serverStub.moveTotem(t)); }
    @Override public void resolveActions(List<String> ids) { execute(() -> serverStub.resolveActions(ids)); }
    @Override public void requestReconnect(String n, String g) {
        this.activeNickname = n; this.activeGameId = g;
        execute(() -> serverStub.requestReconnect(n, g));
    }

    // --- LOGICA DI RICONNESSIONE AUTOMATICA ---

    private synchronized void handleNetworkFailure(Exception e) {
        if (attemptingReconnection) return;

        this.connected = false;
        this.attemptingReconnection = true;

        // Notifichiamo la TUI
        clientView.onError("Connessione persa con il server! Tentativo di ripristino automatico...");

        Thread t = new Thread(() -> {
            System.err.println("[RMI Proxy] Server crash rilevato. Avvio polling di riconnessione...");
            while (!this.connected) {
                try {
                    Thread.sleep(5000); // Prova ogni 5 secondi
                    connect(); // Tenta di rifare il lookup e registrarsi

                    System.out.println("[RMI Proxy] Server tornato online!");

                    // Se stavamo giocando, forziamo il rientro automatico
                    if (activeNickname != null && activeGameId != null) {
                        System.out.println("[RMI Proxy] Ripristino partita " + activeGameId + " per " + activeNickname);
                        serverStub.requestReconnect(activeNickname, activeGameId);
                    }

                    clientView.onError("Server di nuovo online! Partita ripristinata.");
                    this.attemptingReconnection = false;

                } catch (Exception ex) {
                    // Server ancora giù, continua il loop
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
}