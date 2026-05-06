package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.server.controller.persistence.ConnectionStatus;
import it.polimi.ingsw.am02.server.controller.persistence.GameLogger;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.error.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class GameController implements GameObserver {

    // Timeout / delay constants
    private static final long DISCONNECTED_PLAYER_TIMEOUT_SECONDS = 30;
    private static final long GLOBAL_DISCONNECTION_TIMEOUT_SECONDS = 120;
    private static final long DRAIN_DELAY_MILLIS = 5;

    private final String gameId;
    private final ModelInterface model;
    private final Map<String, VirtualView> handlers = new HashMap<>();
    private final GameLogger gameLogger;

    private Runnable gameEndedCallback;

    private final Map<String, ConnectionStatus> connectionStatus = new HashMap<>();
    private final List<Consumer<VirtualView>> globalEventHistory = new CopyOnWriteArrayList<>();
    private final Set<String> gracePeriodExpired = ConcurrentHashMap.newKeySet();

    // Concurrency infrastructure
    private final ExecutorService drainExecutor; // Thread pool for per-player drain loops
    private final ScheduledExecutorService scheduler;
    private final ExecutorService autoPlayerExecutor;

    // Timer state
    private ScheduledFuture<?> disconnectedPlayerTimer;
    private String disconnectedPlayerTimerTarget;
    private ScheduledFuture<?> globalTimer;

    // Turn tracking
    private String currentPlayerNickname;

    // Server-side snapshot for AutoPlayer
    private final ServerGameSnapshot snapshot = new ServerGameSnapshot();

    private boolean replayMode = false;


    public GameController(String gameId, ModelInterface model, Map<String, VirtualView> handlers, GameLogger gameLogger) {
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

        // Register last: fields must be fully initialized before events arrive.
        this.model.addGameObserver(this);
    }

    public synchronized void setGameEndedCallback(Runnable callback) {
        this.gameEndedCallback = callback;
    }

    public synchronized void handle(GameCommand cmd, String senderNickname) {
        log("handle: " + cmd.getClass().getSimpleName() + " from " + senderNickname);
        boolean success = false;
        try {
            switch (cmd) {
                case MoveTotemCommand c      -> model.moveTotem(senderNickname, c.tileID());
                case ResolveActionsCommand c -> model.resolveActions(senderNickname, c.selectedIDs());
            }
            success = true;
        } catch (RuntimeException e) {

            if (replayMode) {
                System.err.println("[GameController:" + gameId + "] REPLAY ERROR: Command rejected for " + senderNickname + " -> " + e.getMessage());
            }

            if (connectionStatus.get(senderNickname) == ConnectionStatus.DISCONNECTED) {
                log("AutoPlayer rejected for " + senderNickname + ": " + e.getMessage());
            } else {
                unicastTransient(senderNickname, v -> v.notifyError(e.getMessage()));
            }
        }
        if (success && !replayMode) {
            gameLogger.logCommand(cmd, senderNickname);
        }
    }

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

        // --- NUOVO CALCOLO --- Considera attivi anche i RECONNECTING
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
                // FASE 1: Inseguimento fuori dal lock
                int currentIndex = 0;
                while (true) {
                    Consumer<VirtualView> historicalCall;
                    synchronized (this) {
                        if (currentIndex >= globalEventHistory.size()) break;
                        historicalCall = globalEventHistory.get(currentIndex);
                    }
                    historicalCall.accept(newView);
                    currentIndex++;
                    Thread.sleep(DRAIN_DELAY_MILLIS);
                }

                // FASE 2: Switch finale atomico
                synchronized (this) {
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

                    if (pendingCount > 0 || (activeCount == 1 && connectionStatus.size() > 1)) {
                        startGlobalDisconnectionTimeout();
                    }
                }

            } catch (Exception e) {
                System.err.println("[GameController:" + gameId + "] Catch-up failed for " + nickname + ": " + e.getMessage());
                handlePlayerDisconnected(nickname);
            }
        });
    }

    synchronized void markAllPlayersPendingReconnection() {
        connectionStatus.replaceAll((nickname, status) -> ConnectionStatus.PENDING_RECONNECTION);
    }

    public synchronized boolean areAllPlayersPendingReconnection() {
        return connectionStatus.values().stream()
                .allMatch(s -> s == ConnectionStatus.PENDING_RECONNECTION);
    }

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

    // 1. Globale — entra nella storia
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

    // 2. Transient privato (errori) — NON entra nella storia
    private synchronized void unicastTransient(String nickname, Consumer<VirtualView> call) {
        if (connectionStatus.get(nickname) == ConnectionStatus.CONNECTED) {
            call.accept(handlers.get(nickname));
        }
    }

    // 3. Transient agli altri (disconnessione, AutoPlayer) — NON entra nella storia
    private synchronized void pushTransientOthers(String exclude, Consumer<VirtualView> call) {
        for (Map.Entry<String, ConnectionStatus> entry : connectionStatus.entrySet()) {
            if (!entry.getKey().equals(exclude)
                    && entry.getValue() == ConnectionStatus.CONNECTED) {
                call.accept(handlers.get(entry.getKey()));
            }
        }
    }

    // Per-player disconnection timer
    private void handleCurrentPlayerTransition(String newCurrentPlayer) {
        this.currentPlayerNickname = newCurrentPlayer;

        // Pulizia timer precedenti
        if (disconnectedPlayerTimer != null && !newCurrentPlayer.equals(disconnectedPlayerTimerTarget)) {
            cancelDisconnectedPlayerTimer(disconnectedPlayerTimerTarget);
        }

        if (connectionStatus.get(newCurrentPlayer) == ConnectionStatus.DISCONNECTED) {
            // --- NUOVO: Fermati se non c'è nessuno ---
            long activeCount = connectionStatus.values().stream()
                    .filter(s -> s == ConnectionStatus.CONNECTED || s == ConnectionStatus.RECONNECTING)
                    .count();
            if (activeCount == 0) {
                log("Game is paused (0 players). Deferring turn timer for " + newCurrentPlayer);
                return;
            }

            // Se il giocatore ha già consumato la sua "grazia", gioca subito
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

        // DELEGHIAMO IL CALCOLO AL THREAD DELL'AUTOPLAYER.
        // In questo modo, il thread aspetterà che il lock venga rilasciato,
        // ovvero che tutti gli eventi (limiti inclusi) siano arrivati nello snapshot!
        autoPlayerExecutor.execute(() -> {

            GameCommand autoCmd;

            // Sincronizziamo la lettura per assicurarci di leggere lo snapshot fresco
            synchronized(this) {
                autoCmd = AutoPlayer.computeMove(nickname, snapshot);
            }

            if (autoCmd == null) {
                log("AutoPlayer produced no command for " + nickname + " in current phase — skipping.");
                return;
            }

            synchronized (this) {
                pushTransientOthers(nickname, v -> v.notifyAutoPlayerInvoked(nickname));
            }

            // Esegue la mossa
            handle(autoCmd, nickname);
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

        // Notify connected players that the grace timer has started
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

    // Global forfeit timer
    private void startGlobalDisconnectionTimeout() {
        if (globalTimer != null)
            return;

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
                    .filter(s -> s == ConnectionStatus.CONNECTED
                            || s == ConnectionStatus.RECONNECTING)
                    .count();

            if (pendingCount == 0 && activeCount > 1) {
                log("Global timer expired but " + activeCount + " players active — skipping.");
                globalTimer = null;
                return;
            }

            globalTimer = null;

            if (pendingCount > 0) {
                // Recovery failed: not all players reconnected in time
                log("Global timer expired — recovery failed, " + pendingCount + " players never reconnected.");
                pushGlobalCall(VirtualView::notifyGameRecoveryFailed);
            } else {
                // Normal flow: one player left, wins by forfeit
                String winner = connectionStatus.entrySet().stream()
                        .filter(e -> e.getValue() == ConnectionStatus.CONNECTED
                                || e.getValue() == ConnectionStatus.RECONNECTING)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElseThrow();
                log("Global timer expired — winner by forfeit: " + winner);
                pushGlobalCall(v -> v.notifyGameAborted(winner));
            }

            gameLogger.logGameEnded();
            gameLogger.close();
            shutdown();
        }

        fireGameEndedCallback();
    }


    // GameObserver callbacks — all synchronized
    @Override
    public synchronized void onGameSetupCompleted(List<String> turnOrder,
                                                  Map<String, Integer> initialFood,
                                                  BoardSnapshot boardSnapshot) {
        snapshot.applySetup(boardSnapshot);
        pushGlobalCall(v -> v.notifyGameSetupCompleted(turnOrder, initialFood, boardSnapshot));
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
    public synchronized void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
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
    public synchronized void onEraChanged(Era newEra,
                                          List<String> newUpperRowBuildings,
                                          List<String> newLowerRowBuildings,
                                          List<String> discardedBuildings) {
        pushGlobalCall(v -> v.notifyEraChanged(newEra, newUpperRowBuildings,
                newLowerRowBuildings, discardedBuildings));
    }

    @Override
    public synchronized void onBoardUpdated(List<String> newUpperRow,
                                            List<String> newLowerRow,
                                            List<String> discardedCards,
                                            List<String> movedToLowerRow,
                                            int deckRemainingCount) {
        snapshot.applyBoardUpdated(newUpperRow, newLowerRow);
        pushGlobalCall(v -> v.notifyBoardUpdated(newUpperRow, newLowerRow,
                discardedCards, movedToLowerRow, deckRemainingCount));
    }

    @Override
    public synchronized void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
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
    public synchronized void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        snapshot.setPlayerLimits(nickname, remainingUpper, remainingLower);
        pushGlobalCall(v -> v.notifyPlayerLimitsInitialized(nickname, remainingUpper, remainingLower));
    }

    @Override
    public synchronized void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        snapshot.setPlayerLimits(nickname, remainingUpper, remainingLower);
        pushGlobalCall(v -> v.notifyPlayerLimitsUpdated(nickname, remainingUpper, remainingLower));

        if (nickname.equals(currentPlayerNickname)
                && connectionStatus.get(nickname) == ConnectionStatus.DISCONNECTED) {
            scheduleAutoPlayerMove(nickname);
        }
    }

    @Override
    public synchronized void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {
        pushGlobalCall(v -> v.notifyPlayerResourceChanged(nickname, resource, newValue, delta));
    }

    @Override
    public synchronized void onEventResolved(String eventID, String eventName) {
        pushGlobalCall(v -> v.notifyEventResolved(eventID, eventName));
    }

    @Override
    public synchronized void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        snapshot.setPlayerLimits(nickname, remainingUpper, remainingLower);
        pushGlobalCall(v -> v.notifyExtraTurnStarted(nickname, remainingUpper, remainingLower));

        if (nickname.equals(currentPlayerNickname)
                && connectionStatus.get(nickname) == ConnectionStatus.DISCONNECTED) {
            scheduleAutoPlayerMove(nickname);
        }
    }

    @Override
    public synchronized void onExtraTurnEnded(String nickname) {
        pushGlobalCall(v -> v.notifyExtraTurnEnded(nickname));
    }

    @Override
    public synchronized void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        pushGlobalCall(v -> v.notifyGameEnded(winners, finalRankings));
        gameLogger.logGameEnded();
        gameLogger.close();
        shutdown();
        fireGameEndedCallback();
    }

    // Utility
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

    public boolean isPlayerDisconnected(String nickname) {
        ConnectionStatus status = connectionStatus.get(nickname);
        return status == ConnectionStatus.DISCONNECTED
                || status == ConnectionStatus.PENDING_RECONNECTION;
    }

    void enterReplayMode() {
        this.replayMode = true;
        connectionStatus.replaceAll((nick, status) -> ConnectionStatus.PENDING_RECONNECTION);
    }

    void exitReplayMode()  { this.replayMode = false; }

}