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

public class GameController implements GameObserver {

    // Timeout / delay constants
    private static final long DISCONNECTED_PLAYER_TIMEOUT_SECONDS = 30;
    private static final long GLOBAL_DISCONNECTION_TIMEOUT_SECONDS = 120;
    private static final long DRAIN_DELAY_MILLIS = 50;

    private final String gameId;
    private final ModelInterface model;
    private final Map<String, VirtualView> handlers;
    private final GameLogger gameLogger;

    private Runnable gameEndedCallback;

    private final Map<String, ConnectionStatus> connectionStatus;
    private final Map<String, Deque<Event>> eventBuffers;

    // Concurrency infrastructure
    private final ExecutorService drainExecutor; // Thread pool for per-player drain loops
    private final ScheduledExecutorService scheduler;

    // Timer state
    private ScheduledFuture<?> disconnectedPlayerTimer;
    private String disconnectedPlayerTimerTarget;
    private ScheduledFuture<?> globalTimer;

    // Turn tracking
    private String currentPlayerNickname;


    public GameController(String gameId, ModelInterface model,
                          Map<String, VirtualView> handlers, GameLogger gameLogger) {

        this.gameId = gameId;
        this.model = model;
        this.handlers = new HashMap<>();
        this.gameLogger = gameLogger;
        this.gameEndedCallback = null;

        this.connectionStatus = new HashMap<>();
        this.eventBuffers = new HashMap<>();
        for (String nickname : handlers.keySet()) {
            connectionStatus.put(nickname, ConnectionStatus.CONNECTED);
            eventBuffers.put(nickname, new ArrayDeque<>());
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
        try {
            switch (cmd) {
                case MoveTotemCommand c -> model.moveTotem(senderNickname, c.tileID());
                case ResolveActionsCommand c -> model.resolveActions(senderNickname, c.selectedIDs());
            }

            gameLogger.logCommand(cmd);

        } catch (RuntimeException e) {
            System.err.println("[GameController:" + gameId + "] Exception in handle: " + e.getMessage());
            e.printStackTrace();
            unicast(senderNickname, new ErrorEvent(e.getMessage()));
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

        broadcastOthers(nickname, new PlayerDisconnectedEvent(nickname));

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

    public synchronized void handlePlayerReconnected(String nickname, VirtualView newView) {
        ConnectionStatus current = connectionStatus.get(nickname);
        if (current == null) {
            System.err.println("[GameController:" + gameId + "] Reconnect for unknown player: " + nickname);
            return;
        }
        if (current == ConnectionStatus.CONNECTED) {
            System.err.println("[GameController:" + gameId + "] Reconnect for already-connected player: " + nickname);
            return;
        }
        if (current == ConnectionStatus.RECONNECTING) {
            System.err.println("[GameController:" + gameId + "] Already RECONNECTING: " + nickname + " (ignored)");
            return;
        }

        handlers.put(nickname, newView);
        cancelDisconnectedPlayerTimer(nickname);
        cancelGlobalDisconnectionTimer();

        connectionStatus.put(nickname, ConnectionStatus.RECONNECTING);
        log("Player reconnecting: " + nickname + " (buffered events: " + eventBuffers.get(nickname).size() + ")");

        broadcastOthers(nickname, new PlayerReconnectedEvent(nickname));
        flushBufferWithDelay(nickname, newView);
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
        log("Shutdown complete.");
    }

  // Private routing (caller must hold this monitor)

    private void unicast(String nickname, Event event) {
        route(nickname, event);
    }

    private void broadcast(Event event) {
        for (String nickname : handlers.keySet()) {
            route(nickname, event);
        }
    }

    private void broadcastOthers(String exclude, Event event) {
        for (String nickname : handlers.keySet()) {
            if (!nickname.equals(exclude)) {
                route(nickname, event);
            }
        }
    }

    private void route(String nickname, Event event) {
        if (connectionStatus.get(nickname) == ConnectionStatus.CONNECTED) {
            handlers.get(nickname).notify(event);
        } else {
            // DISCONNECTED or RECONNECTING: append to buffer.
            eventBuffers.get(nickname).offerLast(event);
        }
    }

    // Drain machinery
    private void flushBufferWithDelay(String nickname, VirtualView newView) {
        drainExecutor.submit(() -> drainLoop(nickname, newView));
    }

    private void drainLoop(String nickname, VirtualView newView) {
        while (true) {
            Event next;
            synchronized (this) {
                Deque<Event> buffer = eventBuffers.get(nickname);

                if (buffer.isEmpty()) {
                    connectionStatus.put(nickname, ConnectionStatus.CONNECTED);
                    log("Drain complete for " + nickname + " — now CONNECTED.");
                    return;
                }

                next = buffer.pollFirst();
            }

            try {
                newView.notify(next);
            } catch (RuntimeException e) {
                System.err.println("[GameController:" + gameId + "] Drain failed for " + nickname + ": " + e.getMessage());

                synchronized (this) {
                    connectionStatus.put(nickname, ConnectionStatus.DISCONNECTED);
                    eventBuffers.get(nickname).offerFirst(next);
                }
                return;
            }
            try {
                Thread.sleep(DRAIN_DELAY_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Per-player disconnection timer
    private void handleCurrentPlayerTransition(String newCurrentPlayer) {
        this.currentPlayerNickname = newCurrentPlayer;
        if (disconnectedPlayerTimer != null && !newCurrentPlayer.equals(disconnectedPlayerTimerTarget)) {
            cancelDisconnectedPlayerTimer(disconnectedPlayerTimerTarget);
        }
        if (connectionStatus.get(newCurrentPlayer) == ConnectionStatus.DISCONNECTED) {
            startDisconnectedPlayerTimer(newCurrentPlayer);
        }
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
            disconnectedPlayerTimer = null;
            disconnectedPlayerTimerTarget = null;
        }
        //TODO
        // Invoked outside the lock so handle() can reacquire it normally.
        // Step 5 placeholder:
        // GameCommand autoCmd = AutoPlayer.computeMove(nickname, ...);
        // handle(autoCmd, nickname);
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
            int activeCount = (int) connectionStatus.values().stream()
                    .filter(s -> s == ConnectionStatus.CONNECTED
                            || s == ConnectionStatus.RECONNECTING)
                    .count();

            if (activeCount > 1) {
                log("Global timer expired but " + activeCount + " players active — skipping.");
                globalTimer = null;
                return;
            }

            String winner = connectionStatus.entrySet().stream()
                    .filter(e -> e.getValue() == ConnectionStatus.CONNECTED
                            || e.getValue() == ConnectionStatus.RECONNECTING)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow();

            log("Global timer expired — winner by forfeit: " + winner);
            globalTimer = null;

            broadcast(new GameAbortedEvent(winner));
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
        broadcast(new GameSetupCompletedEvent(turnOrder, initialFood, boardSnapshot));
    }

    @Override
    public synchronized void onPhaseChanged(PhaseType phase) {
        broadcast(new PhaseChangedEvent(phase, null, null));
    }

    @Override
    public synchronized void onPhaseChanged(PhaseType phase, String currentPlayer) {
        broadcast(new PhaseChangedEvent(phase, currentPlayer, null));
        if (currentPlayer != null)
            handleCurrentPlayerTransition(currentPlayer);
    }

    @Override
    public synchronized void onPhaseChanged(PhaseType phase,
                                            String currentPlayer,
                                            List<String> resolutionOrder) {
        broadcast(new PhaseChangedEvent(phase, currentPlayer, resolutionOrder));
        if (currentPlayer != null) handleCurrentPlayerTransition(currentPlayer);
    }

    @Override
    public synchronized void onCurrentPlayerChanged(String nextPlayer) {
        broadcast(new CurrentPlayerChangedEvent(nextPlayer));
        handleCurrentPlayerTransition(nextPlayer);
    }

    @Override
    public synchronized void onTurnOrderEstablished(List<String> turnOrder) {
        broadcast(new TurnOrderEstablishedEvent(turnOrder));
    }

    @Override
    public synchronized void onEraChanged(Era newEra,
                                          List<String> newUpperRowBuildings,
                                          List<String> newLowerRowBuildings,
                                          List<String> discardedBuildings) {
        broadcast(new EraChangedEvent(newEra, newUpperRowBuildings,
                newLowerRowBuildings, discardedBuildings));
    }

    @Override
    public synchronized void onBoardUpdated(List<String> newUpperRow,
                                            List<String> newLowerRow,
                                            List<String> discardedCards,
                                            List<String> movedToLowerRow,
                                            int deckRemainingCount) {
        broadcast(new BoardUpdatedEvent(newUpperRow, newLowerRow,
                discardedCards, movedToLowerRow, deckRemainingCount));
    }

    @Override
    public synchronized void onCardTaken(String nickname,
                                         String cardID,
                                         CardType cardType,
                                         RowPosition sourceRow) {
        broadcast(new CardTakenEvent(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public synchronized void onTotemPlaced(String nickname, char tileID) {
        broadcast(new TotemPlacedEvent(nickname, tileID));
    }

    @Override
    public synchronized void onTotemReturned(String nickname, int turnOrderPosition) {
        broadcast(new TotemReturnedEvent(nickname, turnOrderPosition));
    }

    @Override
    public synchronized void onPlayerLimitsInitialized(String nickname,
                                                       int remainingUpper,
                                                       int remainingLower) {
        broadcast(new PlayerLimitsInitializedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public synchronized void onPlayerLimitsUpdated(String nickname,
                                                   int remainingUpper,
                                                   int remainingLower) {
        broadcast(new PlayerLimitsUpdatedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public synchronized void onPlayerResourceChanged(String nickname,
                                                     ResourceType resource,
                                                     int newValue,
                                                     int delta) {
        broadcast(new PlayerResourceChangedEvent(nickname, resource, newValue, delta));
    }

    @Override
    public synchronized void onEventResolved(String eventID, String eventName) {
        broadcast(new EventResolvedEvent(eventID, eventName));
    }

    @Override
    public synchronized void onExtraTurnStarted(String nickname,
                                                int remainingUpper,
                                                int remainingLower) {
        broadcast(new ExtraTurnStartedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public synchronized void onExtraTurnEnded(String nickname) {
        broadcast(new ExtraTurnEndedEvent(nickname));
    }

    @Override
    public synchronized void onGameEnded(List<String> winners,
                                         List<PlayerFinalScore> finalRankings) {
        broadcast(new GameEndedEvent(winners, finalRankings));
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
}