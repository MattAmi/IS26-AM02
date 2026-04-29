package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.error.ErrorEvent;
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

    // Intervento 2a: volatile garantisce visibilità tra thread al momento della sostituzione
    private volatile RmiServerRemote serverStub;

    // Intervento 3: il controller crea e possiede il GameModel; il proxy non lo tocca
    private ClientController clientController;

    private volatile boolean connected = false;
    private volatile boolean attemptingReconnection = false;
    private Thread pingThread; // thread che monitora la connessione verso il server
    private static final long PING_INTERVAL_MILLIS = 5000;

    private volatile String activeNickname = null;
    private volatile String activeGameId = null;

    public RmiServerProxy(String host, int port, LobbyModel lobbyModel, ClientView clientView)
            throws RemoteException {
        super();
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.clientView = clientView;
    }

    /**
     * Wires this proxy to the ClientController after construction.
     * Must be called before {@link #connect()}.
     *
     * @param controller the client-side controller that owns the GameModel lifecycle
     */
    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

    @Override
    public void connect() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        RmiServerFactory factory = (RmiServerFactory) registry.lookup("AM02-GameServer");
        this.serverStub = factory.registerClient(this);
        this.connected = true;
        startPingThread();
    }

    @Override
    public void disconnect() {
        this.connected = false;
        stopPingThread();
        try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) {}
    }

    @Override public boolean isConnected() { return connected; }
    @Override public void ping() throws RemoteException { /* Heartbeat dal server */ }

    // --- INBOUND: eventi dal server ---

    @Override
    public void notifyEvent(Event event) throws RemoteException {
        // Aggiorna la cache per la riconnessione
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.activeNickname = e.username();
        }
        if (event instanceof GameStartedEvent e) {
            this.activeGameId = e.gameID();
        }

        if (event instanceof LobbyEvent lobbyEvent) {
            lobbyModel.apply(lobbyEvent);

            // --- CORREZIONE: Crea il GameModel per il setup se siamo in riconnessione ---
            if (clientController.getGameModel() == null && activeNickname != null && activeGameId != null) {
                clientController.onGameModelRequired(activeNickname);
                clientController.getGameModel().setGameId(activeGameId);
            }

            // Passa l'evento al GameModel (fondamentale per GameSetupCompletedEvent!)
            if (clientController.getGameModel() != null) {
                clientController.getGameModel().apply(lobbyEvent);
            }
            // --------------------------------------------------------------------------

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
            if (clientController.getGameModel() != null) {
                clientController.getGameModel().apply(errorEvent);
            } else {
                lobbyModel.apply(errorEvent);
            }
        }
    }

    // --- OUTBOUND: comandi verso il server ---

    private void execute(NetworkAction action) {
        try {
            action.run();
        } catch (RemoteException e) {
            handleNetworkFailure(e);
        }
    }

    @FunctionalInterface
    interface NetworkAction { void run() throws RemoteException; }

    @Override public void requestSetUsername(String u) { execute(() -> serverStub.requestSetUsername(u)); }
    @Override public void requestCreateLobby(int n)   { execute(() -> serverStub.requestCreateLobby(n)); }
    @Override public void requestJoinLobby(String id) { execute(() -> serverStub.requestJoinLobby(id)); }
    @Override public void requestSelectTotem(Totem t) { execute(() -> serverStub.requestSelectTotem(t)); }
    @Override public void requestStartGame()           { execute(() -> serverStub.requestStartGame()); }
    @Override public void requestLeaveLobby()          { execute(() -> serverStub.requestLeaveLobby()); }
    @Override public void moveTotem(char t)            { execute(() -> serverStub.moveTotem(t)); }
    @Override public void resolveActions(List<String> ids) { execute(() -> serverStub.resolveActions(ids)); }

    @Override
    public void requestReconnect(String nickname, String gameId) {
        this.activeNickname = nickname;
        this.activeGameId = gameId;
        execute(() -> serverStub.requestReconnect(nickname, gameId));
    }

    // --- RICONNESSIONE AUTOMATICA ---

    private synchronized void handleNetworkFailure(Exception e) {
        if (attemptingReconnection) return;

        this.connected = false;
        this.attemptingReconnection = true;

        // Intervento 2c: callback semanticamente corretto invece di onError
        clientView.onConnectionLost();

        Thread t = new Thread(() -> {
            stopPingThread(); // ferma il vecchio ping prima di iniziare il loop
            while (!this.connected) {
                try {
                    Thread.sleep(5000);
                    connect(); // connect() avvierà automaticamente un nuovo pingThread

                    if (activeNickname != null && activeGameId != null) {
                        clientController.onGameModelRequired(activeNickname);
                        clientController.getGameModel().setGameId(activeGameId);
                        serverStub.requestReconnect(activeNickname, activeGameId);
                    }

                    clientView.onConnectionRestored();
                    this.attemptingReconnection = false;

                } catch (Exception ex) {
                    // Server ancora giù, continua il loop
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }


    /**
     * Starts a daemon thread that periodically pings the server.
     * If the server does not respond, triggers the reconnection logic.
     * Called automatically by {@link #connect()} after a successful connection.
     */
    private void startPingThread() {
        stopPingThread(); // difensivo: evita doppi thread se connect() viene chiamato due volte
        pingThread = new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(PING_INTERVAL_MILLIS);
                    if (connected) {
                        serverStub.ping(); // RemoteException se il server è down
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (RemoteException e) {
                    if (connected) { // evita di triggerare se disconnect() è già stato chiamato
                        handleNetworkFailure(e);
                    }
                    break;
                }
            }
        });
        pingThread.setDaemon(true);
        pingThread.setName("RmiProxy-PingThread");
        pingThread.start();
    }

    /**
     * Stops the ping thread if it is running.
     */
    private void stopPingThread() {
        if (pingThread != null) {
            pingThread.interrupt();
            pingThread = null;
        }
    }
}