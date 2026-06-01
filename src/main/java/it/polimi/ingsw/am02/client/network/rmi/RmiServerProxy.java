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
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RMI proxy on the client side.
 *
 * <p><b>Inbound</b> (server → client): the server calls {@link RmiClientRemote}
 * methods directly on this object via the RMI thread pool; each call is
 * forwarded synchronously to the {@link ClientNetworkDispatcher}.
 *
 * <p><b>Outbound</b> (client → server): calls from the UI thread are enqueued
 * into a {@link BlockingQueue} and drained by a dedicated worker thread, so
 * the UI never blocks on a slow or unreachable server.
 *
 * <p>Keep-alive is handled by a dedicated ping thread that fires at a fixed
 * interval regardless of outbound traffic, matching the design of
 * {@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy}.
 */
public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClientRemote {

    // ------------------------------------------------------------------
    // Outbound infrastructure
    // ------------------------------------------------------------------

    /**
     * Functional interface equivalent to a {@code Runnable} that may throw
     * {@link RemoteException}, allowing RMI method references in lambdas.
     */
    @FunctionalInterface
    private interface RemoteAction {
        void run() throws RemoteException;
    }

    private final BlockingQueue<RemoteAction> outboundQueue = new LinkedBlockingQueue<>();
    private final Thread outboundWorker;
    private Thread pingThread;

    private volatile boolean running = true;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    private static final long POLL_TIMEOUT_SECONDS = 2;
    private static final long PING_INTERVAL_MILLIS = 5_000;

    // ------------------------------------------------------------------
    // Connection state
    // ------------------------------------------------------------------

    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView clientView;

    private volatile RmiServerRemote serverStub;
    private ClientController clientController;
    private ClientNetworkDispatcher dispatcher;

    private volatile boolean connected = false;
    private volatile boolean attemptingReconnection = false;
    private volatile boolean intentionalDisconnect = false;

    private String activeNickname = null;
    private String activeGameId   = null;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public RmiServerProxy(String host, int port, LobbyModel lobbyModel,
                          ClientView clientView) throws RemoteException {
        super();
        this.host       = host;
        this.port       = port;
        this.lobbyModel = lobbyModel;
        this.clientView = clientView;

        this.outboundWorker = new Thread(this::drainOutboundQueue, "rmi-client-out");
        this.outboundWorker.setDaemon(true);
        this.outboundWorker.start();
    }

    // ------------------------------------------------------------------
    // Outbound drain loop
    // ------------------------------------------------------------------

    /**
     * Drains the outbound queue, executing each enqueued {@link RemoteAction}
     * in order. On {@link RemoteException} triggers the network-failure handler.
     */
    private void drainOutboundQueue() {
        while (running) {
            try {
                RemoteAction action = outboundQueue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (action != null) {
                    action.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RemoteException e) {
                if (connected) {
                    handleNetworkFailure(e);
                }
            }
        }
    }

    /**
     * Enqueues an outbound action. Uses {@link BlockingQueue#add} because the
     * queue is unbounded, so {@link IllegalStateException} is unreachable.
     */
    private void enqueue(RemoteAction action) {
        outboundQueue.add(action);
    }

    // ------------------------------------------------------------------
    // Ping
    // ------------------------------------------------------------------

    private void startPingThread() {
        stopPingThread();
        pingThread = new Thread(() -> {
            while (connected && !intentionalDisconnect) {
                try {
                    Thread.sleep(PING_INTERVAL_MILLIS);
                    if (connected && !intentionalDisconnect) serverStub.ping();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (RemoteException e) {
                    if (connected) handleNetworkFailure(e);
                    break;
                }
            }
        }, "rmi-client-ping");
        pingThread.setDaemon(true);
        pingThread.start();
    }

    private void stopPingThread() {
        if (pingThread != null) {
            pingThread.interrupt();
            pingThread = null;
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void setClientController(ClientController controller) {
        this.clientController = controller;
        this.dispatcher = new ClientNetworkDispatcher(controller, lobbyModel);
    }

    @Override
    public void connect() throws Exception {
        intentionalDisconnect = false;

        try {
            UnicastRemoteObject.exportObject(this, 0);
        } catch (RemoteException ignored) {
            // Already exported on a previous connect() call — safe to ignore.
        }

        RmiServerFactory factory = (RmiServerFactory)
                java.rmi.registry.LocateRegistry.getRegistry(host, port)
                        .lookup("AM02-GameServer");

        this.serverStub = factory.registerClient(this);
        this.connected  = true;
        startPingThread();
    }

    @Override
    public void disconnect() {
        intentionalDisconnect = true;
        this.connected   = false;
        this.activeGameId = null;

        running = false;
        outboundWorker.interrupt();
        stopPingThread();

        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isConnected() { return connected; }

    // ------------------------------------------------------------------
    // Network-failure handler
    // ------------------------------------------------------------------

    private synchronized void handleNetworkFailure(Exception e) {
        if (attemptingReconnection || intentionalDisconnect) return;

        this.connected              = false;
        this.attemptingReconnection = true;
        clientView.onConnectionLost();

        new Thread(() -> {
            while (!connected && !intentionalDisconnect) {
                try {
                    Thread.sleep(5_000);
                    connect();
                    clientView.onConnectionRestored();

                    if (activeNickname != null && activeGameId != null) {
                        if (dispatcher != null) {
                            dispatcher.updateActiveNickname(activeNickname);
                            dispatcher.updateActiveGameId(activeGameId);
                        }
                        enqueue(() -> serverStub.requestReconnect(activeNickname, activeGameId));
                    } else if (activeNickname != null) {
                        enqueue(() -> serverStub.requestSetUsername(activeNickname));
                    }

                    attemptingReconnection = false;
                } catch (Exception ignored) {
                    // Server still offline — retry silently.
                }
            }
        }, "rmi-client-reconnect").start();
    }

    // ------------------------------------------------------------------
    // Outbound — client → server (all enqueued, never blocking the caller)
    // ------------------------------------------------------------------

    @Override
    public void requestSetUsername(String username) {
        enqueue(() -> serverStub.requestSetUsername(username));
    }

    @Override
    public void requestCreateLobby(int numPlayers) {
        enqueue(() -> serverStub.requestCreateLobby(numPlayers));
    }

    @Override
    public void requestJoinLobby(String lobbyID) {
        enqueue(() -> serverStub.requestJoinLobby(lobbyID));
    }

    @Override
    public void requestSelectTotem(Totem totem) {
        enqueue(() -> serverStub.requestSelectTotem(totem));
    }

    @Override
    public void requestLeaveLobby() {
        enqueue(() -> serverStub.requestLeaveLobby());
    }

    @Override
    public void requestReconnect(String nickname, String gameId) {
        this.activeNickname = nickname;
        this.activeGameId   = gameId;

        if (dispatcher != null) {
            dispatcher.updateActiveNickname(nickname);
            dispatcher.updateActiveGameId(gameId);
        }

        enqueue(() -> serverStub.requestReconnect(nickname, gameId));
    }

    @Override
    public void moveTotem(char tileID) {
        enqueue(() -> serverStub.moveTotem(tileID));
    }

    @Override
    public void resolveActions(List<String> selectedIDs) {
        enqueue(() -> serverStub.resolveActions(selectedIDs));
    }

    // ------------------------------------------------------------------
    // Inbound — server → client (called by RMI thread pool, forwarded
    // synchronously to the dispatcher — no queue needed here)
    // ------------------------------------------------------------------

    @Override
    public void ping() throws RemoteException { /* heartbeat — no-op */ }

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
    public void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer,
                                         List<String> t, Map<String, Integer> f,
                                         BoardSnapshot b) throws RemoteException {
        dispatcher.notifyGameSetupCompleted(totemByPlayer, t, f, b);
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
    public void notifyBoardUpdated(List<String> u, List<String> l,
                                   List<String> d, List<String> m,
                                   int dr) throws RemoteException {
        dispatcher.notifyBoardUpdated(u, l, d, m, dr);
    }

    @Override
    public void notifyEraChanged(Era e, List<String> u,
                                 List<String> l, List<String> d) throws RemoteException {
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
    public void notifyCardTaken(String n, String c,
                                CardType t, RowPosition s) throws RemoteException {
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
    public void notifyPlayerResourceChanged(String n, ResourceType r,
                                            int v, int d) throws RemoteException {
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
    public void notifyGameEnded(List<String> w,
                                List<PlayerFinalScore> r) throws RemoteException {
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
    public void notifyAutoPlayerTimerStarted(String n, long seconds) throws RemoteException {
        dispatcher.notifyAutoPlayerTimerStarted(n, seconds);
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

    @Override
    public void notifyGlobalTimerStarted(long seconds) throws RemoteException {
        dispatcher.notifyGlobalTimerStarted(seconds);
    }

    @Override
    public void notifyGlobalTimerCancelled() throws RemoteException {
        dispatcher.notifyGlobalTimerCancelled();
    }
}