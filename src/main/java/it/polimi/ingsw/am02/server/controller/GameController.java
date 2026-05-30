package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.server.controller.persistence.ConnectionStatus;
import it.polimi.ingsw.am02.server.controller.persistence.GameLogger;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.common.messages.commands.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Per-game server-side controller that bridges the network layer and the
 * domain model for a single game session.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Receives granular commands from {@link ControllerManager} and forwards
 *       them to the {@link ModelInterface}.</li>
 *   <li>Implements {@link it.polimi.ingsw.am02.server.model.listeners.GameObserver}
 *       to receive model notifications and fan them out to all connected clients
 *       via {@link it.polimi.ingsw.am02.common.interfaces.VirtualView}.</li>
 *   <li>Maintains a global event history for reconnecting players (catch-up replay).</li>
 *   <li>Manages per-player and global disconnection timers; invokes
 *       {@link AutoPlayer} when a timer expires.</li>
 *   <li>Logs every successfully applied command to a {@link GameLogger} for
 *       crash-recovery replay.</li>
 * </ul>
 *
 * <p>All public methods are {@code synchronized} on the controller's monitor.
 * Background tasks (drain threads, timer callbacks, AutoPlayer) acquire the
 * same lock before reading or writing shared state.
 */
public class GameController implements GameObserver {

    private static final long DISCONNECTED_PLAYER_TIMEOUT_SECONDS = 30;
    private static final long GLOBAL_DISCONNECTION_TIMEOUT_SECONDS = 120;

    private final String gameId;
    private final ModelInterface model;
    private final Map<String, VirtualView> handlers = new HashMap<>();
    private final GameLogger gameLogger;

    private Runnable gameEndedCallback;

    private final Map<String, ConnectionStatus> connectionStatus = new HashMap<>();
    private final List<Consumer<VirtualView>> globalEventHistory = new CopyOnWriteArrayList<>();
    private final Set<String> gracePeriodExpired = ConcurrentHashMap.newKeySet();

    private final ExecutorService drainExecutor;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService autoPlayerExecutor;

    private ScheduledFuture<?> disconnectedPlayerTimer;
    private String disconnectedPlayerTimerTarget;
    private ScheduledFuture<?> globalTimer;

    private String currentPlayerNickname;

    private final ServerGameSnapshot snapshot = new ServerGameSnapshot();

    private boolean replayMode = false;

    /**
     * Constructs a controller for the given game session.
     *
     * @param gameId     unique identifier for this game
     * @param model      the domain model (implements {@link ModelInterface})
     * @param handlers   mapping from player nickname to their {@link VirtualView}
     * @param gameLogger the persistence logger for command recording
     */
    public GameController(String gameId, ModelInterface model,
                          Map<String, VirtualView> handlers, GameLogger gameLogger) {
        this.gameId = gameId;
        this.model = model;
        this.handlers.putAll(handlers);
        this.gameLogger = gameLogger;
        this.gameEndedCallback = null;

        for (String nickname : handlers.keySet()) {
            connectionStatus.put(nickname, ConnectionStatus.CONNECTED);
        }

        this.drainExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "GC[" + gameId + "]-Drain");
            t.setDaemon(true);
            return t;
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GC[" + gameId + "]-Scheduler");
            t.setDaemon(true);
            return t;
        });

        this.autoPlayerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GC[" + gameId + "]-AutoPlayer");
            t.setDaemon(true);
            return t;
        });

        this.disconnectedPlayerTimer = null;
        this.disconnectedPlayerTimerTarget = null;
        this.globalTimer = null;
        this.currentPlayerNickname = null;

        this.model.addGameObserver(this);
    }

    /**
     * Registers a callback invoked once after the game ends (normal finish or forfeit).
     * The callback runs on a daemon thread to avoid blocking the monitor.
     *
     * @param callback the runnable to call when the game session is fully over
     */
    public synchronized void setGameEndedCallback(Runnable callback) {
        this.gameEndedCallback = callback;
    }

    // =========================================================
    // Public granular command methods (called by ControllerManager)
    // =========================================================

    /**
     * Logs and executes a move-totem action on behalf of the given player.
     * Logging is skipped during replay mode, since commands are already
     * persisted in the NDJSON file being replayed.
     *
     * @param nickname the player performing the action
     * @param tileId   the letter identifier of the target offer tile
     */
    public synchronized void executeMoveTotem(String nickname, char tileId) {
        try {
            model.moveTotem(nickname, tileId);
            if (!replayMode) {
                gameLogger.logCommand(new MoveTotemCommand(nickname, tileId), nickname);
                log("Logged MoveTotem for " + nickname + " (tileId=" + tileId + ")");
            }
        } catch (RuntimeException e) {
            handleCommandError(nickname, e);
        }
    }

    /**
     * Logs and executes a resolve-actions action on behalf of the given player.
     * Logging is skipped during replay mode, since commands are already
     * persisted in the NDJSON file being replayed.
     *
     * @param nickname    the player performing the action
     * @param selectedIds the card identifiers the player wishes to take
     */
    public synchronized void executeResolveActions(String nickname, List<String> selectedIds) {
        try {
            model.resolveActions(nickname, selectedIds);
            if (!replayMode) {
                gameLogger.logCommand(new ResolveActionsCommand(nickname, selectedIds), nickname);
                log("Logged ResolveActions for " + nickname + " (selectedIds=" + selectedIds + ")");
            }
        } catch (RuntimeException e) {
            handleCommandError(nickname, e);
        }
    }

    private void handleCommandError(String nickname, RuntimeException e) {
        if (replayMode) {
            System.err.println("[GameController:" + gameId + "] REPLAY ERROR: "
                    + "Command rejected for " + nickname + " -> " + e.getMessage());
            return;
        }
        if (connectionStatus.get(nickname) == ConnectionStatus.DISCONNECTED) {
            log("AutoPlayer rejected for " + nickname + ": " + e.getMessage());
        } else {
            unicastTransient(nickname, v -> v.notifyError(e.getMessage()));
        }
    }

    /**
     * Routes a {@link GameCommand} to the appropriate execution method.
     * Package-private: only used during replay in {@link ControllerManager}.
     * Logging is intentionally omitted here — commands are already logged in the NDJSON file
     * being replayed.
     *
     * @param cmd            the command to execute
     * @param senderNickname the player the command belongs to
     */
    synchronized void handle(GameCommand cmd, String senderNickname) {
        switch (cmd) {
            case MoveTotemCommand c      -> executeMoveTotem(senderNickname, c.tileID());
            case ResolveActionsCommand c -> executeResolveActions(senderNickname, c.selectedIDs());
        }
    }

    /** Returns the {@link GameLogger} for this game. Package-private for use by {@link ControllerManager}. */
    GameLogger getLogger() {
        return gameLogger;
    }

    // =========================================================
    // Disconnection / reconnection
    // =========================================================

    /**
     * Called by the network layer when a player's connection is lost.
     * Updates connection status, notifies other players, and arms the
     * appropriate timer (per-player or global).
     *
     * @param nickname the disconnected player's nickname
     */
    public synchronized void handlePlayerDisconnected(String nickname) {
        if (!connectionStatus.containsKey(nickname)) {
            System.err.println("[GameController:" + gameId + "] Disconnect for unknown player: " + nickname);
            return;
        }

        if (connectionStatus.get(nickname) == ConnectionStatus.DISCONNECTED) {
            return;
        }

        connectionStatus.put(nickname, ConnectionStatus.DISCONNECTED);
        log("Player disconnected: " + nickname);
        pushTransientOthers(nickname, v -> v.notifyPlayerDisconnected(nickname));

        long activeCount = connectionStatus.values().stream()
                .filter(s -> s == ConnectionStatus.CONNECTED || s == ConnectionStatus.RECONNECTING)
                .count();

        if (activeCount == 0) {
            log("Mass disconnection: 0 players active. Pausing game and arming global timer.");
            cancelDisconnectedPlayerTimer(currentPlayerNickname);
            startGlobalDisconnectionTimeout();
        } else if (nickname.equals(currentPlayerNickname)) {
            startDisconnectedPlayerTimer(nickname);
        } else if (activeCount == 1) {
            startGlobalDisconnectionTimeout();
        }
    }

    /**
     * Called by {@link ControllerManager} when a player reconnects.
     * Immediately begins streaming the global event history to the new view on
     * a background thread, then transitions the player to {@code CONNECTED}
     * once the drain is complete.
     *
     * @param nickname the reconnecting player's nickname
     * @param newView  the new {@link VirtualView} for this connection
     * @throws IllegalStateException if the server is still in replay mode and
     *                               cannot yet accept reconnections
     */
    public void handlePlayerReconnected(String nickname, VirtualView newView) {
        synchronized (this) {
            if (replayMode) {
                log("Reconnection rejected for " + nickname + ": server still recovering state.");
                throw new IllegalStateException("Server is still recovering. Please retry in a few seconds.");
            }

            ConnectionStatus current = connectionStatus.get(nickname);
            if (current == null || current == ConnectionStatus.CONNECTED || current == ConnectionStatus.RECONNECTING) {
                return;
            }

            handlers.put(nickname, newView);
            cancelDisconnectedPlayerTimer(nickname);
            cancelGlobalDisconnectionTimer();

            connectionStatus.put(nickname, ConnectionStatus.RECONNECTING);
            log("Player reconnecting: " + nickname);
            gracePeriodExpired.remove(nickname);

            long activeCount = connectionStatus.values().stream()
                    .filter(s -> s == ConnectionStatus.CONNECTED || s == ConnectionStatus.RECONNECTING)
                    .count();

            if (activeCount == 1 && currentPlayerNickname != null) {
                if (connectionStatus.get(currentPlayerNickname) == ConnectionStatus.DISCONNECTED) {
                    if (gracePeriodExpired.contains(currentPlayerNickname)) {
                        log("First human returned. Waking AutoPlayer immediately for " + currentPlayerNickname);
                        scheduleAutoPlayerMove(currentPlayerNickname);
                    } else {
                        log("First human returned. Starting AutoPlayer timer for " + currentPlayerNickname);
                        startDisconnectedPlayerTimer(currentPlayerNickname);
                    }
                }
            }

            pushTransientOthers(nickname, v -> v.notifyPlayerReconnected(nickname));
        }

        drainExecutor.submit(() -> {
            try {
                int currentIndex = 0;

                // Svuota la coda il più velocemente possibile senza bloccare il server
                while (true) {
                    Consumer<VirtualView> historicalCall;
                    synchronized (this) {
                        if (currentIndex >= globalEventHistory.size()) break;
                        historicalCall = globalEventHistory.get(currentIndex);
                    }
                    historicalCall.accept(newView);
                    currentIndex++;
                }

                synchronized (this) {
                    // Assicuriamoci di non aver perso nessun evento dell'ultimo millisecondo
                    while (currentIndex < globalEventHistory.size()) {
                        globalEventHistory.get(currentIndex).accept(newView);
                        currentIndex++;
                    }

                    connectionStatus.put(nickname, ConnectionStatus.CONNECTED);
                    log("Drain complete for " + nickname + " — now CONNECTED.");

                    long pendingCount = connectionStatus.values().stream()
                            .filter(s -> s == ConnectionStatus.PENDING_RECONNECTION)
                            .count();
                    long activeCount = connectionStatus.values().stream()
                            .filter(s -> s == ConnectionStatus.CONNECTED || s == ConnectionStatus.RECONNECTING)
                            .count();

                    if (activeCount >= 2) {
                        // At least 2 players active: game can resume. Cancel the global forfeit timer
                        // and convert any remaining PENDING_RECONNECTION players to DISCONNECTED so
                        // AutoPlayer can act for them. We check activeCount rather than pendingCount
                        // because a concurrent network thread may have already transitioned some
                        // PENDING players to RECONNECTING before this drain completed (race condition).
                        cancelGlobalDisconnectionTimer();
                        connectionStatus.replaceAll((nick, status) ->
                                status == ConnectionStatus.PENDING_RECONNECTION
                                        ? ConnectionStatus.DISCONNECTED
                                        : status);
                        log("Recovery threshold reached (" + activeCount + " players back). "
                                + "Remaining PENDING players converted to DISCONNECTED.");
                        if (currentPlayerNickname != null
                                && connectionStatus.get(currentPlayerNickname) == ConnectionStatus.DISCONNECTED) {
                            if (gracePeriodExpired.contains(currentPlayerNickname)) {
                                scheduleAutoPlayerMove(currentPlayerNickname);
                            } else {
                                startDisconnectedPlayerTimer(currentPlayerNickname);
                            }
                        }
                    } else {
                        // Only one player active so far: arm the 120s global timer.
                        // If a second player's drain completes later, the branch above will cancel it.
                        startGlobalDisconnectionTimeout();
                    }
                }

            } catch (Exception e) {
                System.err.println("[GameController:" + gameId + "] Catch-up failed for "
                        + nickname + ": " + e.getMessage());
                handlePlayerDisconnected(nickname);
            }
        });
    }

    synchronized void markAllPlayersPendingReconnection() {
        connectionStatus.replaceAll((nickname, status) -> ConnectionStatus.PENDING_RECONNECTION);
    }


    /**
     * @return {@code true} if every player is in {@link ConnectionStatus#PENDING_RECONNECTION} state,
     *         meaning the game was recovered from a log but no one has reconnected yet
     */
    public synchronized boolean areAllPlayersPendingReconnection() {
        return connectionStatus.values().stream()
                .allMatch(s -> s == ConnectionStatus.PENDING_RECONNECTION);
    }

    /**
     * Cancels all timers and shuts down the background executor services.
     * Safe to call more than once.
     */
    public synchronized void shutdown() {
        if (disconnectedPlayerTimer != null) {
            disconnectedPlayerTimer.cancel(false);
            disconnectedPlayerTimer = null;
            disconnectedPlayerTimerTarget = null;
        }
        if (globalTimer != null) {
            globalTimer.cancel(false);
            globalTimer = null;
        }
        scheduler.shutdownNow();
        drainExecutor.shutdownNow();
        autoPlayerExecutor.shutdownNow();
        log("Shutdown complete.");
    }

    // =========================================================
    // Notification helpers
    // =========================================================

    private synchronized void pushGlobalCall(Consumer<VirtualView> call) {
        globalEventHistory.add(call);
        for (Map.Entry<String, ConnectionStatus> entry : connectionStatus.entrySet()) {
            String nickname = entry.getKey();
            if (entry.getValue() == ConnectionStatus.CONNECTED) {
                try {
                    call.accept(handlers.get(nickname));
                } catch (RuntimeException e) {
                    log("Failed to notify " + nickname + " during pushGlobalCall.");
                }
            }
        }
    }

    private synchronized void unicastTransient(String nickname, Consumer<VirtualView> call) {
        if (connectionStatus.get(nickname) == ConnectionStatus.CONNECTED) {
            call.accept(handlers.get(nickname));
        }
    }

    private synchronized void pushTransientOthers(String exclude, Consumer<VirtualView> call) {
        for (Map.Entry<String, ConnectionStatus> entry : connectionStatus.entrySet()) {
            if (!entry.getKey().equals(exclude)
                    && entry.getValue() == ConnectionStatus.CONNECTED) {
                call.accept(handlers.get(entry.getKey()));
            }
        }
    }

    // =========================================================
    // Timer management
    // =========================================================

    private void handleCurrentPlayerTransition(String newCurrentPlayer) {
        this.currentPlayerNickname = newCurrentPlayer;

        if (disconnectedPlayerTimer != null && !newCurrentPlayer.equals(disconnectedPlayerTimerTarget)) {
            cancelDisconnectedPlayerTimer(disconnectedPlayerTimerTarget);
        }

        if (connectionStatus.get(newCurrentPlayer) == ConnectionStatus.DISCONNECTED) {
            long activeCount = connectionStatus.values().stream()
                    .filter(s -> s == ConnectionStatus.CONNECTED || s == ConnectionStatus.RECONNECTING)
                    .count();
            if (activeCount == 0) {
                log("Game is paused (0 players). Deferring turn timer for " + newCurrentPlayer);
                return;
            }

            if (gracePeriodExpired.contains(newCurrentPlayer)) {
                log("Player " + newCurrentPlayer + " is still absent. Invoking AutoPlayer immediately.");
                scheduleAutoPlayerMove(newCurrentPlayer);
            } else {
                startDisconnectedPlayerTimer(newCurrentPlayer);
            }
        }
    }

    private void scheduleAutoPlayerMove(String nickname) {
        long activeCount = connectionStatus.values().stream()
                .filter(s -> s == ConnectionStatus.CONNECTED || s == ConnectionStatus.RECONNECTING)
                .count();
        if (activeCount == 0) {
            log("AutoPlayer aborted: no human players connected.");
            return;
        }

        autoPlayerExecutor.execute(() -> {
            GameCommand autoCmd;
            synchronized (this) {
                if (!nickname.equals(currentPlayerNickname)
                        || connectionStatus.get(nickname) != ConnectionStatus.DISCONNECTED) {
                    log("AutoPlayer skipped for " + nickname + ": no longer disconnected or current player.");
                    return;
                }
                autoCmd = AutoPlayer.computeMove(nickname, snapshot);
            }

            if (autoCmd == null) {
                log("AutoPlayer produced no command for " + nickname + " in current phase — skipping.");
                return;
            }

            synchronized (this) {
                pushTransientOthers(nickname, v -> v.notifyAutoPlayerInvoked(nickname));
            }

            handle(autoCmd, nickname);

            // Se il comando era un pick (non move T), rischedula per il pick successivo
            synchronized (this) {
                if (nickname.equals(currentPlayerNickname)
                        && connectionStatus.get(nickname) == ConnectionStatus.DISCONNECTED
                        && autoCmd instanceof ResolveActionsCommand) {
                    scheduleAutoPlayerMove(nickname);
                }
            }
        });
    }

    private void startDisconnectedPlayerTimer(String nickname) {
        if (disconnectedPlayerTimer != null) {
            disconnectedPlayerTimer.cancel(false);
        }
        disconnectedPlayerTimerTarget = nickname;
        disconnectedPlayerTimer = scheduler.schedule(
                () -> onDisconnectedPlayerTimerExpired(nickname),
                DISCONNECTED_PLAYER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log("Per-player timer armed for " + nickname
                + " (" + DISCONNECTED_PLAYER_TIMEOUT_SECONDS + "s).");
        pushTransientOthers(nickname, v -> v.notifyAutoPlayerTimerStarted(nickname));
    }

    private void cancelDisconnectedPlayerTimer(String nickname) {
        if (disconnectedPlayerTimer != null
                && nickname != null
                && nickname.equals(disconnectedPlayerTimerTarget)) {
            disconnectedPlayerTimer.cancel(false);
            disconnectedPlayerTimer = null;
            disconnectedPlayerTimerTarget = null;
            log("Per-player timer cancelled for " + nickname + ".");
        }
    }

    private void onDisconnectedPlayerTimerExpired(String nickname) {
        synchronized (this) {
            if (connectionStatus.get(nickname) != ConnectionStatus.DISCONNECTED) {
                log("Per-player timer expired but " + nickname + " is no longer disconnected — skipping.");
                return;
            }
            if (!nickname.equals(currentPlayerNickname)) {
                log("Per-player timer expired but " + nickname + " is no longer current player — skipping.");
                return;
            }
            log("Per-player timer expired for " + nickname + " — invoking AutoPlayer.");
            gracePeriodExpired.add(nickname);
            disconnectedPlayerTimer = null;
            disconnectedPlayerTimerTarget = null;
        }
        scheduleAutoPlayerMove(nickname);
    }

    private void startGlobalDisconnectionTimeout() {
        if (globalTimer != null) return;
        globalTimer = scheduler.schedule(
                this::onGlobalDisconnectionTimerExpired,
                GLOBAL_DISCONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log("Global forfeit timer armed (" + GLOBAL_DISCONNECTION_TIMEOUT_SECONDS + "s).");
    }

    private void cancelGlobalDisconnectionTimer() {
        if (globalTimer != null) {
            globalTimer.cancel(false);
            globalTimer = null;
            log("Global forfeit timer cancelled.");
        }
    }

    private void onGlobalDisconnectionTimerExpired() {
        synchronized (this) {
            int pendingCount = (int) connectionStatus.values().stream()
                    .filter(s -> s == ConnectionStatus.PENDING_RECONNECTION)
                    .count();
            int activeCount = (int) connectionStatus.values().stream()
                    .filter(s -> s == ConnectionStatus.CONNECTED || s == ConnectionStatus.RECONNECTING)
                    .count();

            if (pendingCount == 0 && activeCount > 1) {
                log("Global timer expired but " + activeCount + " players active — skipping.");
                globalTimer = null;
                return;
            }

            globalTimer = null;

            // In both recovery (pendingCount > 0, activeCount == 1) and normal single-survivor
            // scenarios, the winner is the sole active player.
            String winner = connectionStatus.entrySet().stream()
                    .filter(e -> e.getValue() == ConnectionStatus.CONNECTED
                            || e.getValue() == ConnectionStatus.RECONNECTING)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (winner != null) {
                log("Global timer expired — winner by forfeit: " + winner
                        + " (pendingCount=" + pendingCount + ", activeCount=" + activeCount + ").");
                pushGlobalCall(v -> v.notifyGameAborted(winner));
            } else {
                // Defensive fallback: timer fired with nobody active (should never happen).
                log("Global timer expired with no active player — notifying recovery failed.");
                pushGlobalCall(VirtualView::notifyGameRecoveryFailed);
            }

            gameLogger.logGameEnded();
            gameLogger.close();
            shutdown();
        }
        fireGameEndedCallback();
    }

    // =========================================================
    // GameObserver callbacks
    // =========================================================

    @Override
    public synchronized void onGameSetupCompleted(Map<String, Totem> totemByPlayer,
                                                  List<String> turnOrder,
                                                  Map<String, Integer> initialFood,
                                                  BoardSnapshot boardSnapshot) {
        snapshot.applySetup(boardSnapshot);
        pushGlobalCall(v -> v.notifyGameSetupCompleted(totemByPlayer, turnOrder, initialFood, boardSnapshot));
    }

    @Override
    public synchronized void onPhaseChanged(PhaseType phase) {
        snapshot.setPhase(phase, null);
        pushGlobalCall(v -> v.notifyPhaseChanged(phase, null, null));
    }

    @Override
    public synchronized void onPhaseChanged(PhaseType phase, String currentPlayer) {
        snapshot.setPhase(phase, currentPlayer);
        pushGlobalCall(v -> v.notifyPhaseChanged(phase, currentPlayer, null));
        if (currentPlayer != null)
            handleCurrentPlayerTransition(currentPlayer);
    }

    @Override
    public synchronized void onPhaseChanged(PhaseType phase, String currentPlayer,
                                            List<String> resolutionOrder) {
        snapshot.setPhase(phase, currentPlayer);
        pushGlobalCall(v -> v.notifyPhaseChanged(phase, currentPlayer, resolutionOrder));
        if (currentPlayer != null)
            handleCurrentPlayerTransition(currentPlayer);
    }

    @Override
    public synchronized void onCurrentPlayerChanged(String nextPlayer) {
        snapshot.setCurrentPlayer(nextPlayer);
        pushGlobalCall(v -> v.notifyCurrentPlayerChanged(nextPlayer));
        handleCurrentPlayerTransition(nextPlayer);
    }

    @Override
    public synchronized void onTurnOrderEstablished(List<String> turnOrder) {
        pushGlobalCall(v -> v.notifyTurnOrderEstablished(turnOrder));
    }

    @Override
    public synchronized void onEraChanged(Era newEra, List<String> newUpperRowBuildings,
                                          List<String> newLowerRowBuildings,
                                          List<String> discardedBuildings) {
        pushGlobalCall(v -> v.notifyEraChanged(newEra, newUpperRowBuildings,
                newLowerRowBuildings, discardedBuildings));
    }

    @Override
    public synchronized void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow,
                                            List<String> discardedCards,
                                            List<String> movedToLowerRow, int deckRemainingCount) {
        snapshot.applyBoardUpdated(newUpperRow, newLowerRow);
        pushGlobalCall(v -> v.notifyBoardUpdated(newUpperRow, newLowerRow,
                discardedCards, movedToLowerRow, deckRemainingCount));
    }

    @Override
    public synchronized void onCardTaken(String nickname, String cardID,
                                         CardType cardType, RowPosition sourceRow) {
        snapshot.applyCardTaken(cardID);
        pushGlobalCall(v -> v.notifyCardTaken(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public synchronized void onTotemPlaced(String nickname, char tileID) {
        snapshot.applyTotemPlaced(nickname, tileID);
        pushGlobalCall(v -> v.notifyTotemPlaced(nickname, tileID));
    }

    @Override
    public synchronized void onTotemReturned(String nickname, int turnOrderPosition) {
        snapshot.applyTotemReturned(nickname);
        pushGlobalCall(v -> v.notifyTotemReturned(nickname, turnOrderPosition));
    }

    @Override
    public synchronized void onPlayerLimitsInitialized(String nickname,
                                                       int remainingUpper, int remainingLower) {
        snapshot.setPlayerLimits(nickname, remainingUpper, remainingLower);
        pushGlobalCall(v -> v.notifyPlayerLimitsInitialized(nickname, remainingUpper, remainingLower));
    }

    @Override
    public synchronized void onPlayerLimitsUpdated(String nickname,
                                                   int remainingUpper, int remainingLower) {
        snapshot.setPlayerLimits(nickname, remainingUpper, remainingLower);
        pushGlobalCall(v -> v.notifyPlayerLimitsUpdated(nickname, remainingUpper, remainingLower));
    }

    @Override
    public synchronized void onPlayerResourceChanged(String nickname, ResourceType resource,
                                                     int newValue, int delta) {
        pushGlobalCall(v -> v.notifyPlayerResourceChanged(nickname, resource, newValue, delta));
    }

    @Override
    public synchronized void onEventResolved(String eventID, String eventName) {
        pushGlobalCall(v -> v.notifyEventResolved(eventID, eventName));
    }

    @Override
    public synchronized void onExtraTurnStarted(String nickname,
                                                int remainingUpper, int remainingLower) {
        snapshot.setExtraTurnMode(true);
        snapshot.setPlayerLimits(nickname, remainingUpper, remainingLower);
        pushGlobalCall(v -> v.notifyExtraTurnStarted(nickname, remainingUpper, remainingLower));
    }

    @Override
    public synchronized void onExtraTurnEnded(String nickname) {
        snapshot.setExtraTurnMode(false);
        pushGlobalCall(v -> v.notifyExtraTurnEnded(nickname));
    }

    @Override
    public synchronized void onGameEnded(List<String> winners,
                                         List<PlayerFinalScore> finalRankings) {
        pushGlobalCall(v -> v.notifyGameEnded(winners, finalRankings));
        gameLogger.logGameEnded();
        gameLogger.close();
        shutdown();
        fireGameEndedCallback();
    }

    // =========================================================
    // Utility
    // =========================================================

    private void fireGameEndedCallback() {
        Runnable cb = gameEndedCallback;
        if (cb != null) {
            Thread t = new Thread(cb, "GameController[" + gameId + "]-EndCallback");
            t.setDaemon(true);
            t.start();
        }
    }

    private void log(String msg) {
        System.out.println("[GameController:" + gameId + "] " + msg);
    }

    /**
     * @param nickname the player to query
     * @return {@code true} if the player's status is {@link ConnectionStatus#DISCONNECTED}
     *         or {@link ConnectionStatus#PENDING_RECONNECTION}
     */
    public boolean isPlayerDisconnected(String nickname) {
        ConnectionStatus status = connectionStatus.get(nickname);
        return status == ConnectionStatus.DISCONNECTED
                || status == ConnectionStatus.PENDING_RECONNECTION;
    }

    /**
     * Enters replay mode: all players are set to {@link ConnectionStatus#PENDING_RECONNECTION}
     * and logging is suppressed. Called by {@link ControllerManager} before replaying the NDJSON log.
     */
    void enterReplayMode() {
        this.replayMode = true;
        connectionStatus.replaceAll((nick, status) -> ConnectionStatus.PENDING_RECONNECTION);
    }

    /**
     * Exits replay mode, re-enabling logging and allowing reconnections.
     * Called by {@link ControllerManager} after the NDJSON replay is complete.
     */
    void exitReplayMode() {
        this.replayMode = false;
    }
}