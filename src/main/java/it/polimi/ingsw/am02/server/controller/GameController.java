package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.server.controller.persistence.ConnectionStatus;
import it.polimi.ingsw.am02.server.controller.persistence.GameLogger;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.error.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Orchestrates a single game instance. Manages player connections,
 * time-outs, and routes commands to the domain model.
 */
public class GameController implements GameObserver {

    // Constants for timeouts
    private static final long DISCONNECTED_PLAYER_TIMEOUT_SECONDS = 30;
    private static final long GLOBAL_DISCONNECTION_TIMEOUT_SECONDS = 120;
    private static final long DRAIN_DELAY_MILLIS = 50;

    private final String gameId;
    private final ModelInterface model;
    private final Map<String, VirtualView> handlers = new HashMap<>();
    private final GameLogger gameLogger;

    private Runnable gameEndedCallback;

    private final Map<String, ConnectionStatus> connectionStatus = new HashMap<>();
    private final List<Event> globalEventHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> playerSyncIndex = new ConcurrentHashMap<>();
    private final Set<String> gracePeriodExpired = ConcurrentHashMap.newKeySet();

    // Concurrency
    private final ExecutorService drainExecutor;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService autoPlayerExecutor;

    private ScheduledFuture<?> disconnectedPlayerTimer;
    private String disconnectedPlayerTimerTarget;
    private ScheduledFuture<?> globalTimer;

    private String currentPlayerNickname;
    private final ServerGameSnapshot snapshot = new ServerGameSnapshot();
    private boolean replayMode = false;

    public GameController(String gameId, ModelInterface model,
                          Map<String, VirtualView> handlers, GameLogger gameLogger) {

        this.gameId = gameId;
        this.model = model;
        this.gameLogger = gameLogger;
        this.gameEndedCallback = null;
        this.handlers.putAll(handlers);

        // SAFELY INITIALIZE CONNECTION STATUS
        for (Map.Entry<String, VirtualView> entry : this.handlers.entrySet()) {
            String nickname = entry.getKey();
            if (entry.getValue() == null) {
                connectionStatus.put(nickname, ConnectionStatus.DISCONNECTED);
            } else {
                connectionStatus.put(nickname, ConnectionStatus.CONNECTED);
            }
            playerSyncIndex.put(nickname, 0);
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

    public synchronized void setGameEndedCallback(Runnable callback) {
        this.gameEndedCallback = callback;
    }

    /** Processes commands from the network layer. */
    public synchronized void handle(GameCommand cmd, String senderNickname) {
        log("handle: " + cmd.getClass().getSimpleName() + " from " + senderNickname);
        try {
            switch (cmd) {
                case MoveTotemCommand c -> model.moveTotem(senderNickname, c.tileID());
                case ResolveActionsCommand c -> model.resolveActions(senderNickname, c.selectedIDs());
            }

            if(!replayMode) {
                gameLogger.logCommand(cmd);
            }

        } catch (RuntimeException e) {
            if (connectionStatus.get(senderNickname) == ConnectionStatus.DISCONNECTED) {
                log("AutoPlayer command rejected for disconnected player " + senderNickname + ": " + e.getMessage());
            } else {
                unicastTransient(senderNickname, new ErrorEvent(e.getMessage()));
            }
        }
    }

    /** Handles network drop detection. */
    public synchronized void handlePlayerDisconnected(String nickname) {
        if (!connectionStatus.containsKey(nickname)) return;
        if (connectionStatus.get(nickname) == ConnectionStatus.DISCONNECTED) return;

        connectionStatus.put(nickname, ConnectionStatus.DISCONNECTED);
        log("Player disconnected: " + nickname);
        pushGlobalEventTransientOthers(nickname, new PlayerDisconnectedEvent(nickname));

        if (nickname.equals(currentPlayerNickname)) {
            startDisconnectedPlayerTimer(nickname);
        }

        int connectedCount = (int) connectionStatus.values().stream()
                .filter(s -> s == ConnectionStatus.CONNECTED)
                .count();

        if (connectedCount == 1) {
            startGlobalDisconnectionTimeout();
        }
    }

    /** Re-binds a player to the game and triggers event catch-up. */
    public void handlePlayerReconnected(String nickname, VirtualView newView) {
        synchronized(this) {
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
        }

        pushGlobalEventTransientOthers(nickname, new PlayerReconnectedEvent(nickname));

        // Start async catch-up process (Drain)
        drainExecutor.submit(() -> {
            try {
                int currentIndex = 0;
                // Phase 1: Replay history
                while (true) {
                    Event historicalEvent = null;
                    synchronized(this) {
                        if (currentIndex < globalEventHistory.size()) {
                            historicalEvent = globalEventHistory.get(currentIndex);
                        }
                    }
                    if (historicalEvent == null) break;
                    newView.notify(historicalEvent);
                    currentIndex++;
                    Thread.sleep(DRAIN_DELAY_MILLIS);
                }

                // Phase 2: Final atomic switch to LIVE
                synchronized(this) {
                    while (currentIndex < globalEventHistory.size()) {
                        newView.notify(globalEventHistory.get(currentIndex));
                        currentIndex++;
                    }
                    playerSyncIndex.put(nickname, currentIndex);
                    connectionStatus.put(nickname, ConnectionStatus.CONNECTED);
                    log("Drain complete for " + nickname + " — now CONNECTED.");

                    long pendingCount = connectionStatus.values().stream()
                            .filter(s -> s == ConnectionStatus.PENDING_RECONNECTION)
                            .count();
                    if (pendingCount > 0) {
                        startGlobalDisconnectionTimeout();
                    }
                }
            } catch (Exception e) {
                log("Catch-up failed for " + nickname + ": " + e.getMessage());
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
        if (disconnectedPlayerTimer != null) disconnectedPlayerTimer.cancel(false);
        if (globalTimer != null) globalTimer.cancel(false);
        scheduler.shutdownNow();
        drainExecutor.shutdownNow();
        autoPlayerExecutor.shutdownNow();
        log("Shutdown complete.");
    }

    private synchronized void pushGlobalEvent(Event event) {
        globalEventHistory.add(event);
        int newIndex = globalEventHistory.size();
        for (Map.Entry<String, ConnectionStatus> entry : connectionStatus.entrySet()) {
            if (entry.getValue() == ConnectionStatus.CONNECTED) {
                try {
                    handlers.get(entry.getKey()).notify(event);
                    playerSyncIndex.put(entry.getKey(), newIndex);
                } catch (RuntimeException e) {
                    log("Failed to notify " + entry.getKey() + " during pushGlobalEvent.");
                }
            }
        }
    }

    private synchronized void unicastTransient(String nickname, Event event) {
        if (connectionStatus.get(nickname) == ConnectionStatus.CONNECTED) {
            handlers.get(nickname).notify(event);
        }
    }

    private synchronized void pushGlobalEventTransientOthers(String exclude, Event event) {
        for (Map.Entry<String, ConnectionStatus> entry : connectionStatus.entrySet()) {
            if (!entry.getKey().equals(exclude) && entry.getValue() == ConnectionStatus.CONNECTED) {
                handlers.get(entry.getKey()).notify(event);
            }
        }
    }

    private void handleCurrentPlayerTransition(String newCurrentPlayer) {
        this.currentPlayerNickname = newCurrentPlayer;
        if (disconnectedPlayerTimer != null && !newCurrentPlayer.equals(disconnectedPlayerTimerTarget)) {
            cancelDisconnectedPlayerTimer(disconnectedPlayerTimerTarget);
        }
        if (connectionStatus.get(newCurrentPlayer) == ConnectionStatus.DISCONNECTED) {
            if (gracePeriodExpired.contains(newCurrentPlayer)) {
                log("Player " + newCurrentPlayer + " is still absent. Invoking AutoPlayer immediately.");
                scheduleAutoPlayerMove(newCurrentPlayer);

            } else {
                startDisconnectedPlayerTimer(newCurrentPlayer);
            }
        }
    }

    private void scheduleAutoPlayerMove(String nickname) {
        GameCommand autoCmd = AutoPlayer.computeMove(nickname, snapshot);
        if (autoCmd == null) {
            log("AutoPlayer produced no command for " + nickname + " in current phase — skipping.");
            return;
        }
        // Eseguito fuori dal lock tramite lo autoPlayerExecutor per evitare deadlock:
        // handle() è synchronized, e questo metodo viene chiamato da callback
        // già dentro il monitor (onPlayerLimitsUpdated, handleCurrentPlayerTransition).
        autoPlayerExecutor.execute(() -> handle(autoCmd, nickname));
    }


    private void startDisconnectedPlayerTimer(String nickname) {
        if (disconnectedPlayerTimer != null) disconnectedPlayerTimer.cancel(false);
        disconnectedPlayerTimerTarget = nickname;
        disconnectedPlayerTimer = scheduler.schedule(() -> onDisconnectedPlayerTimerExpired(nickname),
                DISCONNECTED_PLAYER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log("Timer armed for " + nickname + " (" + DISCONNECTED_PLAYER_TIMEOUT_SECONDS + "s).");
    }

    private void cancelDisconnectedPlayerTimer(String nickname) {
        if (disconnectedPlayerTimer != null && nickname != null && nickname.equals(disconnectedPlayerTimerTarget)) {
            disconnectedPlayerTimer.cancel(false);
            disconnectedPlayerTimer = null;
            log("Timer cancelled for " + nickname + ".");
        }
    }

    private void onDisconnectedPlayerTimerExpired(String nickname) {
        synchronized (this) {
            if (connectionStatus.get(nickname) != ConnectionStatus.DISCONNECTED || !nickname.equals(currentPlayerNickname)) return;
            log("Timer expired for " + nickname + " — invoking AutoPlayer.");
            gracePeriodExpired.add(nickname);

            disconnectedPlayerTimer = null;
            disconnectedPlayerTimerTarget = null;
        }

        scheduleAutoPlayerMove(nickname);
    }

    private void startGlobalDisconnectionTimeout() {
        if (globalTimer != null) return;
        globalTimer = scheduler.schedule(this::onGlobalDisconnectionTimerExpired,
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
                pushGlobalEvent(new GameRecoveryFailedEvent());
            } else {
                // Normal flow: one player left, wins by forfeit
                String winner = connectionStatus.entrySet().stream()
                        .filter(e -> e.getValue() == ConnectionStatus.CONNECTED
                                || e.getValue() == ConnectionStatus.RECONNECTING)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElseThrow();
                log("Global timer expired — winner by forfeit: " + winner);
                pushGlobalEvent(new GameAbortedEvent(winner));
            }

            gameLogger.logGameEnded();
            gameLogger.close();
            shutdown();
        }
        fireGameEndedCallback();
    }

    // Domain Observer Callbacks
    @Override public synchronized void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
        snapshot.applySetup(boardSnapshot);
        pushGlobalEvent(new GameSetupCompletedEvent(turnOrder, initialFood, boardSnapshot));
    }
    @Override public synchronized void onPhaseChanged(PhaseType phase) {
        snapshot.setPhase(phase, null);
        pushGlobalEvent(new PhaseChangedEvent(phase, null, null));
    }
    @Override public synchronized void onPhaseChanged(PhaseType phase, String currentPlayer) {
        snapshot.setPhase(phase, currentPlayer);
        pushGlobalEvent(new PhaseChangedEvent(phase, currentPlayer, null));
        if (currentPlayer != null) handleCurrentPlayerTransition(currentPlayer);
    }
    @Override public synchronized void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        snapshot.setPhase(phase, currentPlayer);
        pushGlobalEvent(new PhaseChangedEvent(phase, currentPlayer, resolutionOrder));
        if (currentPlayer != null) handleCurrentPlayerTransition(currentPlayer);
    }
    @Override public synchronized void onCurrentPlayerChanged(String nextPlayer) {
        snapshot.setCurrentPlayer(nextPlayer);
        pushGlobalEvent(new CurrentPlayerChangedEvent(nextPlayer));
        handleCurrentPlayerTransition(nextPlayer);
    }
    @Override public synchronized void onTurnOrderEstablished(List<String> turnOrder) { pushGlobalEvent(new TurnOrderEstablishedEvent(turnOrder)); }
    @Override public synchronized void onEraChanged(Era era, List<String> upper, List<String> lower, List<String> discarded) {
        pushGlobalEvent(new EraChangedEvent(era, upper, lower, discarded));
    }
    @Override public synchronized void onBoardUpdated(List<String> upper, List<String> lower, List<String> discarded, List<String> moved, int deck) {
        snapshot.applyBoardUpdated(upper, lower);
        pushGlobalEvent(new BoardUpdatedEvent(upper, lower, discarded, moved, deck));
    }
    @Override public synchronized void onCardTaken(String nick, String id, CardType type, RowPosition row) {
        snapshot.applyCardTaken(id);
        pushGlobalEvent(new CardTakenEvent(nick, id, type, row));
    }
    @Override public synchronized void onTotemPlaced(String nick, char tile) {
        snapshot.applyTotemPlaced(nick, tile);
        pushGlobalEvent(new TotemPlacedEvent(nick, tile));
    }
    @Override public synchronized void onTotemReturned(String nick, int pos) {
        snapshot.applyTotemReturned(nick);
        pushGlobalEvent(new TotemReturnedEvent(nick, pos));
    }
    @Override
    public synchronized void onPlayerLimitsUpdated(String nickname,
                                                   int remainingUpper,
                                                   int remainingLower) {
        snapshot.setPlayerLimits(nickname, remainingUpper, remainingLower);
        pushGlobalEvent(new PlayerLimitsUpdatedEvent(nickname, remainingUpper, remainingLower));

        if (nickname.equals(currentPlayerNickname)
                && connectionStatus.get(nickname) == ConnectionStatus.DISCONNECTED) {
            scheduleAutoPlayerMove(nickname);
        }
    }
    @Override public synchronized void onPlayerLimitsInitialized(String nick, int u, int l) {
        snapshot.setPlayerLimits(nick, u, l);
        pushGlobalEvent(new PlayerLimitsInitializedEvent(nick, u, l));
    }
    @Override public synchronized void onPlayerLimitsUpdated(String nick, int u, int l) {
        snapshot.setPlayerLimits(nick, u, l);
        pushGlobalEvent(new PlayerLimitsUpdatedEvent(nick, u, l));
    }
    @Override public synchronized void onPlayerResourceChanged(String nick, ResourceType res, int val, int delta) {
        pushGlobalEvent(new PlayerResourceChangedEvent(nick, res, val, delta));
    }
    @Override public synchronized void onEventResolved(String id, String name) { pushGlobalEvent(new EventResolvedEvent(id, name)); }
    @Override public synchronized void onExtraTurnStarted(String nick, int u, int l) {
        snapshot.setPlayerLimits(nick, u, l);
        pushGlobalEvent(new ExtraTurnStartedEvent(nick, u, l));
    }
    @Override public synchronized void onExtraTurnEnded(String nick) { pushGlobalEvent(new ExtraTurnEndedEvent(nick)); }
    @Override public synchronized void onGameEnded(List<String> winners, List<PlayerFinalScore> rankings) {
        pushGlobalEvent(new GameEndedEvent(winners, rankings));
        gameLogger.logGameEnded();
        gameLogger.close();
        shutdown();
        fireGameEndedCallback();
    }

    private void fireGameEndedCallback() {
        Runnable cb = gameEndedCallback;
        if (cb != null) new Thread(cb).start();
    }

    private void log(String msg) { System.out.println("[GameController:" + gameId + "] " + msg); }

    public boolean isPlayerDisconnected(String nickname) {
        ConnectionStatus status = connectionStatus.get(nickname);
        return status == ConnectionStatus.DISCONNECTED
                || status == ConnectionStatus.PENDING_RECONNECTION;
    }

    void enterReplayMode() { this.replayMode = true; }
    void exitReplayMode()  { this.replayMode = false; }
}