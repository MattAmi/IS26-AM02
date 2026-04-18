package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.server.model.exceptions.GameRuleException;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;

import java.util.List;
import java.util.Map;

public class GameController implements GameObserver {

    private final ModelInterface model;
    private final Map<String, VirtualView> handlers;


    public GameController(ModelInterface model, Map<String, VirtualView> handlers) {
        this.model = model;
        this.handlers = handlers;
        this.model.addGameObserver(this);
    }


    //Routing methods
    private void unicast(String nickname, Event event) {
        VirtualView h = handlers.get(nickname);
        if (h != null)
            h.notify(event);
    }

    private void broadcast(Event event) {
        handlers.values().forEach(h -> h.notify(event));
    }

    private void broadcastOthers(String excludeNickname, Event event) {
        handlers.entrySet().stream()
                .filter(e -> !e.getKey().equals(excludeNickname))
                .forEach(e -> e.getValue().notify(event));
    }


    //Command handler
    public void handle(Command cmd, String senderNickname) {
        try {
            switch (cmd) {
                case MoveTotemCommand c -> model.moveTotem(senderNickname, c.tileID());
                case ResolveActionsCommand c -> model.resolveActions(senderNickname, c.cardIDs());
            }
        } catch (GameRuleException e) {
            unicast(new ErrorEvent(e.getMessage()), senderNickname);

        } catch (RuntimeException e) {
            System.err.println("Server error: ");
            e.printStackTrace();
            unicast(senderNickname, new ErrorEvent("Internal server error. Please contact an admin"));
        }
    }


    //GameObserver implementation (event translation and dispatching)
    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
        broadcast(new GameSetupCompletedEvent(turnOrder, initialFood, boardSnapshot));
    }

    @Override
    public void onPhaseChanged(PhaseType phase) {
        // Uso null per i parametri opzionali non presenti in questo overload
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
    public void onSustainmentResolved(String nickname, int totalCharacters, int foodPaid, int foodShortage, int ppLost) {
        broadcast(new SustainmentResolvedEvent(nickname, totalCharacters, foodPaid, foodShortage, ppLost));
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
    public void onFinalScoreCalculated(String nickname, int ppFromBuilders, int ppFromBuildings, int ppFromInventors, int ppFromArtists, int ppFromBuildingEffects, int totalPrestigePoints, int remainingFood) {
        broadcast(new FinalScoreCalculatedEvent(nickname, ppFromBuilders, ppFromBuildings, ppFromInventors, ppFromArtists, ppFromBuildingEffects, totalPrestigePoints, remainingFood));
    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        broadcast(new GameEndedEvent(winners, finalRankings));
    }
}
