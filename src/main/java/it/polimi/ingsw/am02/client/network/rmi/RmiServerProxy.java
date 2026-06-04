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
     * A {@link Runnable}-like interface whose single method may throw
     * {@link RemoteException}, enabling RMI method references to be stored
     * as lambdas in the outbound queue.
     */
    @FunctionalInterface
    private interface RemoteAction {
        void run() throws RemoteException;
    }

    /** Unbounded queue of outbound actions waiting to be sent to the server. */
    private final BlockingQueue<RemoteAction> outboundQueue = new LinkedBlockingQueue<>();

    /** Dedicated thread that drains {@link #outboundQueue} sequentially. */
    private final Thread outboundWorker;

    /** Dedicated thread that sends periodic pings regardless of outbound traffic. */
    private Thread pingThread;

    /** {@code false} once {@link #disconnect()} has been called. */
    private volatile boolean running = true;

    /** Guards against multiple concurrent calls to {@link #handleNetworkFailure}. */
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    /** Maximum time the outbound worker waits for the next queued action. */
    private static final long POLL_TIMEOUT_SECONDS = 2;

    /** Interval between consecutive pings, in milliseconds. */
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

    /** Set to {@code true} by {@link #disconnect()} to suppress reconnection attempts. */
    private volatile boolean intentionalDisconnect = false;

    /** Nickname last successfully registered with the server; used for reconnection. */
    private String activeNickname = null;

    /** Game ID currently joined; used for reconnection. */
    private String activeGameId   = null;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    /**
     * Creates an RMI proxy ready to be wired and connected.
     *
     * <p>The outbound worker thread is started immediately; no connection is
     * established until {@link #connect()} is called.
     *
     * @param host       the server hostname or IP address
     * @param port       the RMI registry port
     * @param lobbyModel the lobby model that receives pre-game notifications
     * @param clientView the view used to signal connection-state changes
     * @throws RemoteException if the {@link UnicastRemoteObject} constructor fails
     */
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
     * Main loop of the outbound worker thread.
     *
     * <p>Polls {@link #outboundQueue} with a timeout so the thread can
     * terminate cleanly when {@link #running} is set to {@code false}.
     * Any {@link RemoteException} thrown by an action triggers
     * {@link #handleNetworkFailure}.
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
     * Adds an outbound action to the queue.
     *
     * <p>{@link BlockingQueue#add} is used instead of {@code offer}/{@code put}
     * because the queue is unbounded; {@link IllegalStateException} is unreachable.
     *
     * @param action the remote call to enqueue
     */
    private void enqueue(RemoteAction action) {
        outboundQueue.add(action);
    }

    // ------------------------------------------------------------------
    // Ping
    // ------------------------------------------------------------------

    /**
     * Starts a new ping thread, stopping any previously running one first.
     *
     * <p>The thread fires at {@link #PING_INTERVAL_MILLIS} intervals
     * and triggers {@link #handleNetworkFailure} on {@link RemoteException}.
     */
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

    /**
     * Interrupts and discards the current ping thread, if any.
     */
    private void stopPingThread() {
        if (pingThread != null) {
            pingThread.interrupt();
            pingThread = null;
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void setClientController(ClientController controller) {
        this.clientController = controller;
        this.dispatcher = new ClientNetworkDispatcher(controller, lobbyModel);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public boolean isConnected() { return connected; }

    // ------------------------------------------------------------------
    // Network-failure handler
    // ------------------------------------------------------------------

    /**
     * Handles an unexpected loss of connectivity.
     *
     * <p>Marks the connection as down, notifies the view, and spawns a
     * reconnection thread that retries every 5 seconds. Once the connection
     * is re-established the thread replays the login and game-rejoin sequence
     * using {@link #activeNickname} and {@link #activeGameId}.
     *
     * <p>Synchronized to prevent concurrent executions triggered by both the
     * outbound worker and the ping thread.
     *
     * @param e the exception that caused the failure (used for logging purposes)
     */
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
    // Outbound — client → server
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void requestSetUsername(String username) {
        enqueue(() -> serverStub.requestSetUsername(username));
    }

    /** {@inheritDoc} */
    @Override
    public void requestCreateLobby(int numPlayers) {
        enqueue(() -> serverStub.requestCreateLobby(numPlayers));
    }

    /** {@inheritDoc} */
    @Override
    public void requestJoinLobby(String lobbyID) {
        enqueue(() -> serverStub.requestJoinLobby(lobbyID));
    }

    /** {@inheritDoc} */
    @Override
    public void requestSelectTotem(Totem totem) {
        enqueue(() -> serverStub.requestSelectTotem(totem));
    }

    /** {@inheritDoc} */
    @Override
    public void requestLeaveLobby() {
        enqueue(() -> serverStub.requestLeaveLobby());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Also updates {@link #activeNickname} and {@link #activeGameId} so that
     * the automatic reconnection loop can replay this call after a network failure.
     */
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

    /** {@inheritDoc} */
    @Override
    public void moveTotem(char tileID) {
        enqueue(() -> serverStub.moveTotem(tileID));
    }

    /** {@inheritDoc} */
    @Override
    public void resolveActions(List<String> selectedIDs) {
        enqueue(() -> serverStub.resolveActions(selectedIDs));
    }

    // ------------------------------------------------------------------
    // Inbound — server → client
    // ------------------------------------------------------------------

    /**
     * Heartbeat called by the server at regular intervals.
     * No-op on the client side; the fact that the call succeeded is
     * sufficient proof that the connection is alive.
     */
    @Override
    public void ping() throws RemoteException { /* heartbeat — no-op */ }

    /**
     * {@inheritDoc}
     *
     * <p>Also caches the validated username in {@link #activeNickname} for
     * use by the automatic reconnection loop.
     */
    @Override
    public void notifyUsernameResult(String u, boolean v, String r) throws RemoteException {
        if (v) this.activeNickname = u;
        dispatcher.notifyUsernameResult(u, v, r);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Also caches the game ID in {@link #activeGameId} so that the
     * automatic reconnection loop can rejoin the correct game.
     */
    @Override
    public void notifyGameStarted(String id) throws RemoteException {
        this.activeGameId = id;
        dispatcher.notifyGameStarted(id);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyAvailableLobbiesUpdated(List<LobbyInfo> l) throws RemoteException {
        dispatcher.notifyAvailableLobbiesUpdated(l);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCurrentLobbyUpdated(LobbyInfo l) throws RemoteException {
        dispatcher.notifyCurrentLobbyUpdated(l);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyLobbyDissolved(String id) throws RemoteException {
        dispatcher.notifyLobbyDissolved(id);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer,
                                         List<String> t, Map<String, Integer> f,
                                         BoardSnapshot b) throws RemoteException {
        dispatcher.notifyGameSetupCompleted(totemByPlayer, t, f, b);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPhaseChanged(PhaseType p, String c, List<String> r) throws RemoteException {
        dispatcher.notifyPhaseChanged(p, c, r);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCurrentPlayerChanged(String n) throws RemoteException {
        dispatcher.notifyCurrentPlayerChanged(n);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTurnOrderEstablished(List<String> t) throws RemoteException {
        dispatcher.notifyTurnOrderEstablished(t);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyBoardUpdated(List<String> u, List<String> l,
                                   List<String> d, List<String> m,
                                   int dr) throws RemoteException {
        dispatcher.notifyBoardUpdated(u, l, d, m, dr);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEraChanged(Era e, List<String> u,
                                 List<String> l, List<String> d) throws RemoteException {
        dispatcher.notifyEraChanged(e, u, l, d);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTotemPlaced(String n, char t) throws RemoteException {
        dispatcher.notifyTotemPlaced(n, t);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTotemReturned(String n, int p) throws RemoteException {
        dispatcher.notifyTotemReturned(n, p);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCardTaken(String n, String c,
                                CardType t, RowPosition s) throws RemoteException {
        dispatcher.notifyCardTaken(n, c, t, s);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerLimitsInitialized(String n, int u, int l) throws RemoteException {
        dispatcher.notifyPlayerLimitsInitialized(n, u, l);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerLimitsUpdated(String n, int u, int l) throws RemoteException {
        dispatcher.notifyPlayerLimitsUpdated(n, u, l);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerResourceChanged(String n, ResourceType r,
                                            int v, int d) throws RemoteException {
        dispatcher.notifyPlayerResourceChanged(n, r, v, d);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEventResolved(String i, String n) throws RemoteException {
        dispatcher.notifyEventResolved(i, n);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyExtraTurnStarted(String n, int u, int l) throws RemoteException {
        dispatcher.notifyExtraTurnStarted(n, u, l);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyExtraTurnEnded(String n) throws RemoteException {
        dispatcher.notifyExtraTurnEnded(n);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameEnded(List<String> w,
                                List<PlayerFinalScore> r) throws RemoteException {
        dispatcher.notifyGameEnded(w, r);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyError(String m) throws RemoteException {
        dispatcher.notifyError(m);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerDisconnected(String n) throws RemoteException {
        dispatcher.notifyPlayerDisconnected(n);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerReconnected(String n) throws RemoteException {
        dispatcher.notifyPlayerReconnected(n);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyAutoPlayerTimerStarted(String n, long seconds) throws RemoteException {
        dispatcher.notifyAutoPlayerTimerStarted(n, seconds);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyAutoPlayerInvoked(String n) throws RemoteException {
        dispatcher.notifyAutoPlayerInvoked(n);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameAborted(String w) throws RemoteException {
        dispatcher.notifyGameAborted(w);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameRecoveryFailed() throws RemoteException {
        dispatcher.notifyGameRecoveryFailed();
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGlobalTimerStarted(long seconds) throws RemoteException {
        dispatcher.notifyGlobalTimerStarted(seconds);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGlobalTimerCancelled() throws RemoteException {
        dispatcher.notifyGlobalTimerCancelled();
    }
}