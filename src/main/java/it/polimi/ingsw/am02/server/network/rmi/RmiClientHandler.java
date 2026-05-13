package it.polimi.ingsw.am02.server.network.rmi;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;
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
 * <p>Inbound RMI calls (client → server) are executed directly on the RMI
 * thread pool — no extra executor needed, because the client's outbound queue
 * already breaks the synchronicity chain on the other end.
 *
 * <p>Outbound notifications (server → client) are queued as {@link RmiCall}
 * lambdas and drained by a dedicated worker thread, so that
 * {@link it.polimi.ingsw.am02.server.controller.GameController} never blocks
 * on a slow or dead client while holding its lock.
 *
 * <p>Keep-alive is handled by a dedicated ping thread that fires at a fixed
 * interval regardless of outbound traffic, matching the design of
 * {@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler}.
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

    private final VirtualControllerManager manager;
    private final RmiClientRemote stub;
    private final String clientId;

    private final BlockingQueue<RmiCall> outboundQueue = new LinkedBlockingQueue<>();
    private final Thread outboundWorker;
    private Thread pingThread;

    private volatile boolean running = true;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    private static final long POLL_TIMEOUT_SECONDS  = 2;
    private static final long PING_INTERVAL_MILLIS  = 5_000;

    public RmiClientHandler(RmiClientRemote stub) {
        this.manager = ControllerManager.getInstance();
        this.stub = stub;
        this.clientId = manager.handleClientConnected(this);
        this.outboundWorker = new Thread(this::drainOutboundQueue, "rmi-out-" + clientId);
        this.outboundWorker.setDaemon(true);
        this.outboundWorker.start();
        startPingThread();
    }

    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    /**
     * Adds a call to the outbound queue. Uses {@link BlockingQueue#add} instead
     * of {@code offer} to avoid the "result ignored" warning; with an unbounded
     * {@link LinkedBlockingQueue} the queue can never be full, so
     * {@link IllegalStateException} is unreachable in practice.
     */
    private void enqueue(RmiCall call) {
        outboundQueue.add(call);
    }

    private void startPingThread() {
        pingThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(PING_INTERVAL_MILLIS);
                    if (running) stub.ping();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (RemoteException e) {
                    System.err.println("[RmiClientHandler] Ping failed for client "
                            + clientId + ": " + e.getMessage());
                    disconnect();
                    break;
                }
            }
        }, "rmi-ping-" + clientId);
        pingThread.setDaemon(true);
        pingThread.start();
    }

    // =========================================================
    // DRAIN LOOP — runs on outboundWorker thread
    // =========================================================

    /**
     * Drains the outbound queue, executing each enqueued {@link RmiCall} in order.
     * On {@link RemoteException} the client is considered unreachable and
     * {@link #disconnect()} is called.
     */
    private void drainOutboundQueue() {
        while (running) {
            try {
                RmiCall call = outboundQueue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (call != null) {
                    call.invoke(stub);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RemoteException e) {
                System.err.println("[RmiClientHandler] Client " + clientId
                        + " unreachable: " + e.getMessage());
                disconnect();
                break;
            }
        }
    }

    // =========================================================
    // VirtualView — outbound notifications (server → client)
    // =========================================================

    @Override
    public void notifyUsernameResult(String username, boolean isValid, String reason) {
        enqueue(s -> s.notifyUsernameResult(username, isValid, reason));
    }

    @Override
    public void notifyGameStarted(String gameId) {
        enqueue(s -> s.notifyGameStarted(gameId));
    }

    @Override
    public void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        enqueue(s -> s.notifyAvailableLobbiesUpdated(lobbies));
    }

    @Override
    public void notifyCurrentLobbyUpdated(LobbyInfo lobby) {
        enqueue(s -> s.notifyCurrentLobbyUpdated(lobby));
    }

    @Override
    public void notifyLobbyDissolved(String lobbyID) {
        enqueue(s -> s.notifyLobbyDissolved(lobbyID));
    }

    @Override
    public void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer,
                                         List<String> turnOrder,
                                         Map<String, Integer> initialFood,
                                         BoardSnapshot boardSnapshot) {
        enqueue(s -> s.notifyGameSetupCompleted(totemByPlayer, turnOrder,
                initialFood, boardSnapshot));
    }

    @Override
    public void notifyPhaseChanged(PhaseType phase, String currentPlayer,
                                   List<String> resolutionOrder) {
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
    public void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow,
                                   List<String> discardedCards, List<String> movedToLowerRow,
                                   int deckRemainingCount) {
        enqueue(s -> s.notifyBoardUpdated(newUpperRow, newLowerRow, discardedCards,
                movedToLowerRow, deckRemainingCount));
    }

    @Override
    public void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings,
                                 List<String> newLowerRowBuildings,
                                 List<String> discardedBuildings) {
        enqueue(s -> s.notifyEraChanged(newEra, newUpperRowBuildings,
                newLowerRowBuildings, discardedBuildings));
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
    public void notifyCardTaken(String nickname, String cardID, CardType cardType,
                                RowPosition sourceRow) {
        enqueue(s -> s.notifyCardTaken(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public void notifyPlayerLimitsInitialized(String nickname, int remainingUpper,
                                              int remainingLower) {
        enqueue(s -> s.notifyPlayerLimitsInitialized(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyPlayerLimitsUpdated(String nickname, int remainingUpper,
                                          int remainingLower) {
        enqueue(s -> s.notifyPlayerLimitsUpdated(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyPlayerResourceChanged(String nickname, ResourceType resource,
                                            int newValue, int delta) {
        enqueue(s -> s.notifyPlayerResourceChanged(nickname, resource, newValue, delta));
    }

    @Override
    public void notifyEventResolved(String eventID, String eventName) {
        enqueue(s -> s.notifyEventResolved(eventID, eventName));
    }

    @Override
    public void notifyExtraTurnStarted(String nickname, int remainingUpper,
                                       int remainingLower) {
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
    // RmiServerRemote — inbound calls (client → server)
    // Executed directly on the RMI thread pool — no executor wrapper.
    // =========================================================

    @Override
    public void requestSetUsername(String username) throws RemoteException {
        manager.requestSetUsername(clientId, username);
    }

    @Override
    public void requestCreateLobby(int numPlayers) throws RemoteException {
        manager.requestCreateLobby(clientId, numPlayers);
    }

    @Override
    public void requestJoinLobby(String lobbyID) throws RemoteException {
        manager.requestJoinLobby(clientId, lobbyID);
    }

    @Override
    public void requestSelectTotem(Totem color) throws RemoteException {
        manager.requestSelectTotem(clientId, color);
    }

    @Override
    public void requestLeaveLobby() throws RemoteException {
        manager.requestLeaveLobby(clientId);
    }

    @Override
    public void requestReconnect(String nickname, String gameId) throws RemoteException {
        manager.requestReconnect(clientId, this, nickname, gameId);
    }

    @Override
    public void moveTotem(char tileID) throws RemoteException {
        manager.requestMoveTotem(clientId, tileID);
    }

    @Override
    public void resolveActions(List<String> selectedIDs) throws RemoteException {
        manager.requestResolveActions(clientId, selectedIDs);
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
        outboundWorker.interrupt();
        if (pingThread != null) pingThread.interrupt();
        manager.handleDisconnection(clientId);
    }
}