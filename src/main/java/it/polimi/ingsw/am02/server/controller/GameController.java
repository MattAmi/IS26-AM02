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

    private final ModelInterface model;
    private final Map<String, VirtualView> handlers;

    private final GameLogger gameLogger;
    private final Map<String, ConnectionStatus> connectionStatus;
    private final Map<String, Queue<Event>> eventBuffers;

    public GameController(ModelInterface model, Map<String, VirtualView> handlers, GameLogger gameLogger) {
        this.model = model;
        this.handlers = handlers;
        this.model.addGameObserver(this);
        this.gameLogger = gameLogger;

        this.connectionStatus = new HashMap<>();
        this.eventBuffers = new HashMap<>();

        for (String nickname : handlers.keySet()) {
            connectionStatus.put(nickname, ConnectionStatus.CONNECTED);
            eventBuffers.put(nickname, new ArrayDeque<>());
        }
    }


    //Routing methods
    private void unicast(String nickname, Event event) {
        route(nickname, event);
    }

    private void broadcast(Event event) {
        handlers.keySet()
                .forEach(nickname -> route(nickname, event));
    }

    private void broadcastOthers(String excludeNickname, Event event) {
        handlers.keySet().stream()
                .filter(nickname -> !nickname.equals(excludeNickname))
                .forEach(nickname -> route(nickname, event));
    }

    private void route(String nickname, Event event) {
        if (connectionStatus.get(nickname) == ConnectionStatus.CONNECTED) {
            handlers.get(nickname).notify(event);
        } else {
            eventBuffers.get(nickname).offer(event);
        }
    }

    //Command handler
    public void handle(GameCommand cmd, String senderNickname) {
        System.out.println("[GameController] handle: " + cmd.getClass().getSimpleName() + " from " + senderNickname);
        try {
            switch (cmd) {
                case MoveTotemCommand c -> model.moveTotem(senderNickname, c.tileID());
                case ResolveActionsCommand c -> model.resolveActions(senderNickname, c.selectedIDs());
            }

            gameLogger.logCommand(cmd);

        } catch (RuntimeException e) {
            System.err.println("[GameController] Exception: " + e.getMessage());
            e.printStackTrace();
            unicast(senderNickname, new ErrorEvent(e.getMessage()));
        }
    }

    public void handlePlayerDisconnected(String nickname) {
        connectionStatus.put(nickname, ConnectionStatus.DISCONNECTED);
        broadcastOthers(nickname, new PlayerDisconnectedEvent(nickname));

        //TODO
    }

    private void startGlobalDisconnectionTimeout() {
    }

    private void onDisconnectedPlayerTimerExpired(String nickname) {
    }

    private void onGlobalDisconnectionTimerExpired() {
    }

    public void handlePlayerReconnected(String nickname, VirtualView newView) {
    }

    private void cancelDisconnectedPlayerTimer(String nickname) {
    }

    private void cancelGlobalDisconnectionTimer() {
    }

    private void flushBufferWithDelay(String nickname, VirtualView newView) {

    }

    //GameObserver implementation (event translation and dispatching)
    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
        broadcast(new GameSetupCompletedEvent(turnOrder, initialFood, boardSnapshot));
    }

    @Override
    public void onPhaseChanged(PhaseType phase) {
        broadcast(new PhaseChangedEvent(phase, null, null));
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer) {
        broadcast(new PhaseChangedEvent(phase, currentPlayer, null));
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        broadcast(new PhaseChangedEvent(phase, currentPlayer, resolutionOrder));
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        broadcast(new CurrentPlayerChangedEvent(nextPlayer));
    }

    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) {
        broadcast(new TurnOrderEstablishedEvent(turnOrder));
    }

    @Override
    public void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {
        broadcast(new EraChangedEvent(newEra, newUpperRowBuildings, newLowerRowBuildings, discardedBuildings));
    }

    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {
        broadcast(new BoardUpdatedEvent(newUpperRow, newLowerRow, discardedCards, movedToLowerRow, deckRemainingCount));
    }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        broadcast(new CardTakenEvent(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        broadcast(new TotemPlacedEvent(nickname, tileID));
    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        broadcast(new TotemReturnedEvent(nickname, turnOrderPosition));
    }

    @Override
    public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        broadcast(new PlayerLimitsInitializedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        broadcast(new PlayerLimitsUpdatedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {
        broadcast(new PlayerResourceChangedEvent(nickname, resource, newValue, delta));
    }

    @Override
    public void onEventResolved(String eventID, String eventName) {
        broadcast(new EventResolvedEvent(eventID, eventName));
    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        broadcast(new ExtraTurnStartedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onExtraTurnEnded(String nickname) {
        broadcast(new ExtraTurnEndedEvent(nickname));
    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        broadcast(new GameEndedEvent(winners, finalRankings));
        gameLogger.logGameEnded();
        gameLogger.close();
    }
}
