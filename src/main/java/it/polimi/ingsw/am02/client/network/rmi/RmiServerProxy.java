package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ClientNetworkDispatcher;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerFactory;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

/**
 * RMI proxy on the client side. Receives calls from server (Inbound) and sends commands (Outbound).
 */
public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClientRemote {

    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView clientView;

    private volatile RmiServerRemote serverStub;
    private ClientController clientController;
    private ClientNetworkDispatcher dispatcher;

    private volatile boolean connected = false;
    private volatile boolean attemptingReconnection = false;
    private Thread pingThread;
    private static final long PING_INTERVAL_MILLIS = 5000;

    // Cache for automatic reconnection
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
    public void setClientController(ClientController controller) {
        this.clientController = controller;
        this.dispatcher = new ClientNetworkDispatcher(controller, lobbyModel);
    }

    @Override
    public void connect() throws Exception {
        try { UnicastRemoteObject.exportObject(this, 1099); } catch (RemoteException ignored) {}
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
    @Override public void ping() throws RemoteException { /* Heartbeat */ }

    // INBOUND (From server via RMI)

    @Override
    public void notifyUsernameResult(String u, boolean v, String r) throws RemoteException {
        if (v) this.activeNickname = u;
        dispatcher.notifyUsernameResult(u, v, r);
    }

    @Override
    public void notifyGameStarted(String id) throws RemoteException {
        this.activeGameId = id;
        dispatcher.notifyGameStarted(id);
    }

    @Override
    public void notifyAvailableLobbiesUpdated(List<LobbyInfo> l) throws RemoteException {
        dispatcher.notifyAvailableLobbiesUpdated(l);
    }

    @Override
    public void notifyCurrentLobbyUpdated(LobbyInfo l) throws RemoteException {
        dispatcher.notifyCurrentLobbyUpdated(l);
    }

    @Override
    public void notifyLobbyDissolved(String id) throws RemoteException {
        dispatcher.notifyLobbyDissolved(id);
    }

    @Override
    public void notifyGameSetupCompleted(List<String> t, Map<String, Integer> f, BoardSnapshot b) throws RemoteException {
        dispatcher.notifyGameSetupCompleted(t, f, b);
    }

    @Override
    public void notifyPhaseChanged(PhaseType p, String c, List<String> r) throws RemoteException {
        dispatcher.notifyPhaseChanged(p, c, r);
    }

    @Override
    public void notifyCurrentPlayerChanged(String n) throws RemoteException {
        dispatcher.notifyCurrentPlayerChanged(n);
    }

    @Override
    public void notifyTurnOrderEstablished(List<String> t) throws RemoteException {
        dispatcher.notifyTurnOrderEstablished(t);
    }

    @Override
    public void notifyBoardUpdated(List<String> u, List<String> l, List<String> d, List<String> m, int dr) throws RemoteException {
        dispatcher.notifyBoardUpdated(u, l, d, m, dr);
    }

    @Override
    public void notifyEraChanged(Era e, List<String> u, List<String> l, List<String> d) throws RemoteException {
        dispatcher.notifyEraChanged(e, u, l, d);
    }

    @Override
    public void notifyTotemPlaced(String n, char t) throws RemoteException {
        dispatcher.notifyTotemPlaced(n, t);
    }

    @Override
    public void notifyTotemReturned(String n, int p) throws RemoteException {
        dispatcher.notifyTotemReturned(n, p);
    }

    @Override
    public void notifyCardTaken(String n, String c, CardType t, RowPosition s) throws RemoteException {
        dispatcher.notifyCardTaken(n, c, t, s);
    }

    @Override
    public void notifyPlayerLimitsInitialized(String n, int u, int l) throws RemoteException {
        dispatcher.notifyPlayerLimitsInitialized(n, u, l);
    }

    @Override
    public void notifyPlayerLimitsUpdated(String n, int u, int l) throws RemoteException {
        dispatcher.notifyPlayerLimitsUpdated(n, u, l);
    }

    @Override
    public void notifyPlayerResourceChanged(String n, ResourceType r, int v, int d) throws RemoteException {
        dispatcher.notifyPlayerResourceChanged(n, r, v, d);
    }

    @Override
    public void notifyEventResolved(String i, String n) throws RemoteException {
        dispatcher.notifyEventResolved(i, n);
    }

    @Override
    public void notifyExtraTurnStarted(String n, int u, int l) throws RemoteException {
        dispatcher.notifyExtraTurnStarted(n, u, l);
    }

    @Override
    public void notifyExtraTurnEnded(String n) throws RemoteException {
        dispatcher.notifyExtraTurnEnded(n);
    }

    @Override
    public void notifyGameEnded(List<String> w, List<PlayerFinalScore> r) throws RemoteException {
        dispatcher.notifyGameEnded(w, r);
    }

    @Override
    public void notifyError(String m) throws RemoteException {
        dispatcher.notifyError(m);
    }

    @Override
    public void notifyPlayerDisconnected(String n) throws RemoteException {
        dispatcher.notifyPlayerDisconnected(n);
    }

    @Override
    public void notifyPlayerReconnected(String n) throws RemoteException {
        dispatcher.notifyPlayerReconnected(n);
    }

    @Override
    public void notifyAutoPlayerTimerStarted(String n) throws RemoteException {
        dispatcher.notifyAutoPlayerTimerStarted(n);
    }

    @Override
    public void notifyAutoPlayerInvoked(String n) throws RemoteException {
        dispatcher.notifyAutoPlayerInvoked(n);
    }

    @Override
    public void notifyGameAborted(String w) throws RemoteException {
        dispatcher.notifyGameAborted(w);
    }

    @Override
    public void notifyGameRecoveryFailed() throws RemoteException {
        dispatcher.notifyGameRecoveryFailed();
    }


    // OUTBOUND (From client to server)

    private void execute(RemoteAction action) {
        try { action.run(); } catch (RemoteException e) { handleNetworkFailure(e); }
    }

    @FunctionalInterface interface RemoteAction { void run() throws RemoteException; }

    @Override public void requestSetUsername(String u)   { execute(() -> serverStub.requestSetUsername(u)); }
    @Override public void requestCreateLobby(int n)      { execute(() -> serverStub.requestCreateLobby(n)); }
    @Override public void requestJoinLobby(String id)    { execute(() -> serverStub.requestJoinLobby(id)); }
    @Override public void requestSelectTotem(Totem t)    { execute(() -> serverStub.requestSelectTotem(t)); }

    @Override public void requestLeaveLobby()            { execute(() -> serverStub.requestLeaveLobby()); }
    @Override public void moveTotem(char t)              { execute(() -> serverStub.moveTotem(t)); }
    @Override public void resolveActions(List<String> s) { execute(() -> serverStub.resolveActions(s)); }

    @Override
    public void requestReconnect(String n, String g) {
        this.activeNickname = n;
        this.activeGameId = g;

        // Stessa logica: allineiamo il dispatcher locale
        if (this.dispatcher != null) {
            this.dispatcher.updateActiveNickname(n);
            this.dispatcher.updateActiveGameId(g);
        }

        execute(() -> serverStub.requestReconnect(n, g));
    }

    // --- RECONNECTION AND PING ---

    private synchronized void handleNetworkFailure(Exception e) {
        if (attemptingReconnection) return;
        this.connected = false;
        this.attemptingReconnection = true;
        clientView.onConnectionLost();

        new Thread(() -> {
            stopPingThread();
            while (!this.connected) {
                try {
                    Thread.sleep(5000);
                    connect();
                    if (activeNickname != null && activeGameId != null) {
                        clientView.onConnectionRestored();
                        serverStub.requestReconnect(activeNickname, activeGameId);
                    } else {
                        clientView.onReturnToLobby();
                        // il server manderà notifyAvailableLobbiesUpdated da solo
                    }
                    this.attemptingReconnection = false;
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void startPingThread() {
        stopPingThread();
        pingThread = new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(PING_INTERVAL_MILLIS);
                    if (connected) serverStub.ping();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (RemoteException e) {
                    if (connected) handleNetworkFailure(e);
                    break;
                }
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();
    }

    private void stopPingThread() {
        if (pingThread != null) { pingThread.interrupt(); pingThread = null; }
    }
}