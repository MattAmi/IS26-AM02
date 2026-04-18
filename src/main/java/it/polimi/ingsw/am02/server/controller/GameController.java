package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.server.network.ClientHandler;

import java.util.List;
import java.util.Map;

public class GameController implements GameObserver {

    private final ModelInterface model;
    private final Map<String, ClientHandler> handlers;


    public GameController(ModelInterface model, Map<String, ClientHandler> handlers) {
        this.model = model;
        this.handlers = handlers;
        this.model.addGameObserver(this);
    }


    private void unicast(String nickname, Event event) {
        ClientHandler h = handlers.get(nickname);
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



    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {

    }

    @Override
    public void onPhaseChanged(PhaseType phase) {

    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer) {

    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {

    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {

    }

    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) {

    }

    @Override
    public void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {

    }

    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {

    }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {

    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {

    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {

    }

    @Override
    public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {

    }

    @Override
    public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {

    }

    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {

    }

    @Override
    public void onEventResolved(String eventID, String eventName) {

    }

    @Override
    public void onSustainmentResolved(String nickname, int totalCharacters, int foodPaid, int foodShortage, int ppLost) {

    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {

    }

    @Override
    public void onExtraTurnEnded(String nickname) {

    }

    @Override
    public void onFinalScoreCalculated(String nickname, int ppFromBuilders, int ppFromBuildings, int ppFromInventors, int ppFromArtists, int ppFromBuildingEffects, int totalPrestigePoints, int remainingFood) {

    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {

    }
}
