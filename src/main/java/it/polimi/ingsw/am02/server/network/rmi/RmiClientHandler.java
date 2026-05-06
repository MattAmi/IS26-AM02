package it.polimi.ingsw.am02.server.network.rmi;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.network.ClientHandler;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-side handler for a single RMI-connected client.
 *
 * <p>Inbound RMI calls (client → server) are submitted to a single-thread executor
 * to avoid blocking the RMI thread pool and to serialize command processing.
 *
 * <p>Outbound notifications (server → client) are queued as {@link RmiCall} lambdas
 * and drained by a dedicated thread, so that {@link it.polimi.ingsw.am02.server.controller.GameController}
 * never blocks on a slow or dead client while holding its lock.
 */
public class RmiClientHandler implements RmiServerRemote, ClientHandler {

    /**
     * Functional interface equivalent to {@code Consumer<RmiClientRemote>} but
     * declaring {@link RemoteException}, allowing RMI method references in lambdas.
     */
    @FunctionalInterface
    private interface RmiCall {
        void invoke(RmiClientRemote stub) throws RemoteException;
    }

    private final ControllerManager manager;
    private final RmiClientRemote stub;
    private final String clientId;

    private final ExecutorService inboundExecutor = Executors.newSingleThreadExecutor();
    private final BlockingQueue<RmiCall> outboundQueue = new LinkedBlockingQueue<>();
    private final Thread outboundWorker;

    private volatile boolean running = true;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    private static final long POLL_TIMEOUT_SECONDS = 2;

    public RmiClientHandler(RmiClientRemote stub) {
        this.manager = ControllerManager.getInstance();
        this.stub = stub;
        this.clientId = manager.handleClientConnected(this);
        this.outboundWorker = new Thread(this::drainOutboundQueue, "rmi-out-" + clientId);
        this.outboundWorker.setDaemon(true);
        this.outboundWorker.start();
    }

    // =========================================================
    // INTERNAL HELPER
    // =========================================================

    /**
     * Adds a call to the outbound queue. Uses {@link BlockingQueue#add} instead of
     * {@code offer} to avoid the "result ignored" warning; with an unbounded
     * {@link LinkedBlockingQueue} the queue can never be full, so
     * {@link IllegalStateException} is unreachable in practice.
     */
    private void enqueue(RmiCall call) {
        outboundQueue.add(call);
    }

    // =========================================================
    // DRAIN LOOP — runs on outboundWorker thread
    // =========================================================

    private void drainOutboundQueue() {
        while (running) {
            try {
                RmiCall call = outboundQueue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (call != null) {
                    call.invoke(stub);
                } else {
                    stub.ping();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RemoteException e) {
                System.err.println("[RmiClientHandler] Client " + clientId + " unreachable: " + e.getMessage());
                disconnect();
                break;
            }
        }
    }

    // =========================================================
    // VirtualView — called by GameController (under its lock)
    // Each method enqueues a lambda and returns immediately.
    // =========================================================

    @Override
    public void notifyGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
        enqueue(s -> s.notifyGameSetupCompleted(turnOrder, initialFood, boardSnapshot));
    }

    @Override
    public void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        enqueue(s -> s.notifyPhaseChanged(phase, currentPlayer, resolutionOrder));
    }

    @Override
    public void notifyCurrentPlayerChanged(String nextPlayer) {
        enqueue(s -> s.notifyCurrentPlayerChanged(nextPlayer));
    }

    @Override
    public void notifyTurnOrderEstablished(List<String> turnOrder) {
        enqueue(s -> s.notifyTurnOrderEstablished(turnOrder));
    }

    @Override
    public void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {
        enqueue(s -> s.notifyBoardUpdated(newUpperRow, newLowerRow, discardedCards, movedToLowerRow, deckRemainingCount));
    }

    @Override
    public void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {
        enqueue(s -> s.notifyEraChanged(newEra, newUpperRowBuildings, newLowerRowBuildings, discardedBuildings));
    }

    @Override
    public void notifyTotemPlaced(String nickname, char tileID) {
        enqueue(s -> s.notifyTotemPlaced(nickname, tileID));
    }

    @Override
    public void notifyTotemReturned(String nickname, int turnOrderPosition) {
        enqueue(s -> s.notifyTotemReturned(nickname, turnOrderPosition));
    }

    @Override
    public void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        enqueue(s -> s.notifyCardTaken(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        enqueue(s -> s.notifyPlayerLimitsInitialized(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        enqueue(s -> s.notifyPlayerLimitsUpdated(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {
        enqueue(s -> s.notifyPlayerResourceChanged(nickname, resource, newValue, delta));
    }

    @Override
    public void notifyEventResolved(String eventID, String eventName) {
        enqueue(s -> s.notifyEventResolved(eventID, eventName));
    }

    @Override
    public void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        enqueue(s -> s.notifyExtraTurnStarted(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyExtraTurnEnded(String nickname) {
        enqueue(s -> s.notifyExtraTurnEnded(nickname));
    }

    @Override
    public void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        enqueue(s -> s.notifyGameEnded(winners, finalRankings));
    }

    @Override
    public void notifyError(String message) {
        enqueue(s -> s.notifyError(message));
    }

    @Override
    public void notifyPlayerDisconnected(String nickname) {
        enqueue(s -> s.notifyPlayerDisconnected(nickname));
    }

    @Override
    public void notifyPlayerReconnected(String nickname) {
        enqueue(s -> s.notifyPlayerReconnected(nickname));
    }

    @Override
    public void notifyAutoPlayerTimerStarted(String nickname) {
        enqueue(s -> s.notifyAutoPlayerTimerStarted(nickname));
    }

    @Override
    public void notifyAutoPlayerInvoked(String nickname) {
        enqueue(s -> s.notifyAutoPlayerInvoked(nickname));
    }

    @Override
    public void notifyGameAborted(String winner) {
        enqueue(s -> s.notifyGameAborted(winner));
    }

    @Override
    public void notifyGameRecoveryFailed() {
        enqueue(RmiClientRemote::notifyGameRecoveryFailed);
    }

    // =========================================================
    // RmiServerRemote — inbound commands from client
    // =========================================================

    @Override
    public void requestSetUsername(String username) throws RemoteException {
        inboundExecutor.submit(() -> manager.requestSetUsernameInLobby(clientId, username));
    }

    @Override
    public void requestCreateLobby(int numPlayers) throws RemoteException {
        inboundExecutor.submit(() -> manager.createLobby(clientId, numPlayers));
    }

    @Override
    public void requestJoinLobby(String lobbyID) throws RemoteException {
        inboundExecutor.submit(() -> manager.joinLobby(clientId, lobbyID));
    }

    @Override
    public void requestSelectTotem(Totem color) throws RemoteException {
        inboundExecutor.submit(() -> manager.selectTotem(clientId, color));
    }

    @Override
    public void requestStartGame() throws RemoteException {
        inboundExecutor.submit(() -> manager.routeGameCommand(clientId, new StartGameCommand()));
    }

    @Override
    public void requestLeaveLobby() throws RemoteException {
        inboundExecutor.submit(() -> manager.leaveLobby(clientId));
    }

    @Override
    public void moveTotem(char tileID) throws RemoteException {
        inboundExecutor.submit(() -> manager.routeGameCommand(clientId, new MoveTotemCommand("", tileID)));
    }

    @Override
    public void resolveActions(List<String> selectedIDs) throws RemoteException {
        inboundExecutor.submit(() -> manager.routeGameCommand(clientId, new ResolveActionsCommand("", selectedIDs)));
    }

    @Override
    public void requestReconnect(String nickname, String gameId) throws RemoteException {
        inboundExecutor.submit(() -> manager.handleReconnectRequest(clientId, this, new ReconnectCommand(nickname, gameId)));
    }

    @Override
    public void ping() throws RemoteException {
        // Heartbeat from client to server: no logic needed.
    }

    // =========================================================
    // Lifecycle
    // =========================================================

    @Override
    public void disconnect() {
        if (!disconnected.compareAndSet(false, true)) return;
        running = false;
        inboundExecutor.shutdownNow();
        outboundWorker.interrupt();
        manager.handleDisconnection(clientId);
    }
}