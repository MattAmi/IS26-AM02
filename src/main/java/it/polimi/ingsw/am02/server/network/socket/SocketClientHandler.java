package it.polimi.ingsw.am02.server.network.socket;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.commands.heartbeat.PingCommand;
import it.polimi.ingsw.am02.common.messages.commands.heartbeat.PongCommand;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.heartbeat.PongEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;
import it.polimi.ingsw.am02.common.messages.events.error.*;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.network.ClientHandler;
import it.polimi.ingsw.am02.common.messages.events.heartbeat.PingEvent;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Server-side handler for a single Socket-connected client.
 *
 * <p>Inbound: a reader loop deserializes JSON lines into {@link Command} records
 * and dispatches them via {@link Command#apply(VirtualControllerManager, String)},
 * implementing the Inversion of Control pattern symmetrically to
 * {@link Event#apply} on the client side. {@link Command} records are used solely
 * as the deserialization target; they are never passed downstream.
 *
 * <p>Outbound: each {@link it.polimi.ingsw.am02.common.interfaces.VirtualView} notification
 * constructs the corresponding {@link Event} record, enqueues it, and returns immediately.
 * A dedicated writer thread drains the queue and serializes events to the socket,
 * so {@link it.polimi.ingsw.am02.server.controller.GameController} never blocks on I/O
 * while holding its lock.
 *
 * <p>Keep-alive is handled via periodic {@link PingEvent} sent by a scheduler;
 * disconnection is detected when the client stops sending {@link PongCommand} within the timeout.
 */
public class SocketClientHandler implements ClientHandler {

    private final Socket socket;
    private final JsonMessageCodec codec;
    private final VirtualControllerManager manager;
    private final PrintWriter out;
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile long lastPongReceivedAt = System.currentTimeMillis();
    private volatile String clientId;

    private static final int PING_INTERVAL_SECONDS = 5;
    private static final int PING_TIMEOUT_SECONDS = 10;

    /**
     * Creates a handler for the given socket, registers it with
     * {@link ControllerManager}, and initializes the outbound event queue and
     * ping scheduler.
     *
     * @param socket the accepted client socket
     * @param codec  the codec used to serialize {@link Event} records and
     *               deserialize {@link Command} records as newline-delimited JSON
     * @throws IOException if obtaining the socket's output stream fails
     */
    public SocketClientHandler(Socket socket, JsonMessageCodec codec) throws IOException {
        this.socket = socket;
        this.codec = codec;
        this.manager = ControllerManager.getInstance();
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
        );
        this.clientId = manager.handleClientConnected(this);
        this.lastPongReceivedAt = System.currentTimeMillis();
    }

    /**
     * Starts the outbound writer thread, the ping scheduler, and then enters
     * the blocking reader loop. Returns only when the connection is closed.
     *
     * <p>Must be called from a dedicated thread, as the reader loop blocks
     * until the socket is closed or an I/O error occurs.
     */
    public void listen() {
        Thread writerThread = new Thread(this::writerLoop, "socket-out-" + clientId);
        writerThread.setDaemon(true);
        writerThread.start();
        startPingTimer();
        readerLoop();
    }

    // =========================================================
    // PING / PONG
    // =========================================================

    private void startPingTimer() {
        pingScheduler.scheduleAtFixedRate(
                () -> eventQueue.add(new PingEvent()),
                PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);

        pingScheduler.scheduleAtFixedRate(() -> {
            long elapsed = System.currentTimeMillis() - lastPongReceivedAt;
            if (elapsed > PING_TIMEOUT_SECONDS * 1000L) {
                System.out.println("[SocketClientHandler] Ping timeout for clientId: " + clientId);
                disconnect();
            }
        }, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    // =========================================================
    // READER / WRITER LOOPS
    // =========================================================

    private void readerLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = codec.decode(line);
                if (msg instanceof Command cmd) {
                    dispatch(cmd);
                }
            }
        } catch (IOException e) {
            System.out.println("[SocketClientHandler] Client disconnected: " + clientId);
        } finally {
            disconnect();
        }
    }

    private void writerLoop() {
        try {
            while (!socket.isClosed()) {
                Event event = eventQueue.take();
                out.println(codec.encode(event));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // COMMAND DISPATCH — IoC: each Command knows what to do
    // =========================================================

    private void dispatch(Command cmd) {
        if (cmd instanceof PongCommand) {
            lastPongReceivedAt = System.currentTimeMillis();
            return;
        }
        if (cmd instanceof PingCommand) {
            eventQueue.add(new PongEvent());
            return;
        }
        if (cmd instanceof ReconnectCommand rc) {
            rc.apply(manager, clientId, this);
            return;
        }
        cmd.apply(manager, clientId);
    }

    // =========================================================
    // VirtualView — outbound notifications (server → client)
    // =========================================================

    @Override
    public void notifyUsernameResult(String username, boolean isValid, String reason) {
        eventQueue.add(new UsernameResultEvent(username, isValid, reason));
    }

    @Override
    public void notifyGameStarted(String gameId) {
        eventQueue.add(new GameStartedEvent(gameId));
    }

    @Override
    public void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        eventQueue.add(new UpdatedLobbiesEvent(lobbies));
    }

    @Override
    public void notifyCurrentLobbyUpdated(LobbyInfo lobby) {
        eventQueue.add(new UpdatedLobbyEvent(lobby));
    }

    @Override
    public void notifyLobbyDissolved(String lobbyID) {
        eventQueue.add(new LobbyDissolvedEvent(lobbyID));
    }

    @Override
    public void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer, List<String> turnOrder, Map<String, Integer> initialFood,
                                         BoardSnapshot boardSnapshot) {
        eventQueue.add(new GameSetupCompletedEvent(totemByPlayer, turnOrder, initialFood, boardSnapshot));
    }

    @Override
    public void notifyPhaseChanged(PhaseType phase, String currentPlayer,
                                   List<String> resolutionOrder) {
        eventQueue.add(new PhaseChangedEvent(phase, currentPlayer, resolutionOrder));
    }

    @Override
    public void notifyCurrentPlayerChanged(String nextPlayer) {
        eventQueue.add(new CurrentPlayerChangedEvent(nextPlayer));
    }

    @Override
    public void notifyTurnOrderEstablished(List<String> turnOrder) {
        eventQueue.add(new TurnOrderEstablishedEvent(turnOrder));
    }

    @Override
    public void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow,
                                   List<String> discardedCards, List<String> movedToLowerRow,
                                   int deckRemainingCount) {
        eventQueue.add(new BoardUpdatedEvent(newUpperRow, newLowerRow, discardedCards,
                movedToLowerRow, deckRemainingCount));
    }

    @Override
    public void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings,
                                 List<String> newLowerRowBuildings,
                                 List<String> discardedBuildings) {
        eventQueue.add(new EraChangedEvent(newEra, newUpperRowBuildings,
                newLowerRowBuildings, discardedBuildings));
    }

    @Override
    public void notifyTotemPlaced(String nickname, char tileID) {
        eventQueue.add(new TotemPlacedEvent(nickname, tileID));
    }

    @Override
    public void notifyTotemReturned(String nickname, int turnOrderPosition) {
        eventQueue.add(new TotemReturnedEvent(nickname, turnOrderPosition));
    }

    @Override
    public void notifyCardTaken(String nickname, String cardID, CardType cardType,
                                RowPosition sourceRow) {
        eventQueue.add(new CardTakenEvent(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public void notifyPlayerLimitsInitialized(String nickname, int remainingUpper,
                                              int remainingLower) {
        eventQueue.add(new PlayerLimitsInitializedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        eventQueue.add(new PlayerLimitsUpdatedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyPlayerResourceChanged(String nickname, ResourceType resource,
                                            int newValue, int delta) {
        eventQueue.add(new PlayerResourceChangedEvent(nickname, resource, newValue, delta));
    }

    @Override
    public void notifyEventResolved(String eventID, String eventName) {
        eventQueue.add(new EventResolvedEvent(eventID, eventName));
    }

    @Override
    public void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        eventQueue.add(new ExtraTurnStartedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void notifyExtraTurnEnded(String nickname) {
        eventQueue.add(new ExtraTurnEndedEvent(nickname));
    }

    @Override
    public void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        eventQueue.add(new GameEndedEvent(winners, finalRankings));
    }

    @Override
    public void notifyError(String message) {
        eventQueue.add(new ErrorEvent(message));
    }

    @Override
    public void notifyPlayerDisconnected(String nickname) {
        eventQueue.add(new PlayerDisconnectedEvent(nickname));
    }

    @Override
    public void notifyPlayerReconnected(String nickname) {
        eventQueue.add(new PlayerReconnectedEvent(nickname));
    }

    @Override
    public void notifyAutoPlayerTimerStarted(String nickname, long seconds) { eventQueue.add(new AutoPlayerTimerStartedEvent(nickname, seconds)); }

    @Override
    public void notifyAutoPlayerInvoked(String nickname) {
        eventQueue.add(new AutoPlayerInvokedEvent(nickname));
    }

    @Override
    public void notifyGameAborted(String winner) {
        eventQueue.add(new GameAbortedEvent(winner));
    }

    @Override
    public void notifyGameRecoveryFailed() {
        eventQueue.add(new GameRecoveryFailedEvent());
    }

    @Override
    public void notifyGlobalTimerStarted(long seconds) { eventQueue.add(new GlobalTimerStartedEvent(seconds)); }

    @Override public void notifyGlobalTimerCancelled() { eventQueue.add(new GlobalTimerCancelledEvent()); }

    // =========================================================
    // Lifecycle
    // =========================================================

    /**
     * Closes the socket, shuts down the ping scheduler, and notifies
     * {@link ControllerManager} of the disconnection. Idempotent: the first
     * call nulls out {@code clientId}; subsequent calls are no-ops.
     */
    @Override
    public synchronized void disconnect() {
        if (clientId == null) return;
        String id = clientId;
        clientId = null;
        pingScheduler.shutdownNow();
        try { socket.close(); } catch (IOException ignored) {}
        manager.handleDisconnection(id);
    }
}