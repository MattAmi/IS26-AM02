package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GameNotifier implements GameObserverRegistry, GameEventEmitter {

    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();

    //GameObserverRegistry methods

    @Override
    public void addObserver(GameObserver observer) { observers.add(observer); }

    @Override
    public void removeObserver(GameObserver observer) { observers.remove(observer); }


    //GameEventEmitter methods

    // SetUp
    @Override
    public void notifyGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
        for (GameObserver o : observers)
            o.onGameSetupCompleted(turnOrder, initialFood, boardSnapshot);
    }

    // TotemPlacement
    @Override
    public void notifyTotemPlaced(String nickname, char tileID) {
        for (GameObserver o : observers)
            o.onTotemPlaced(nickname, tileID);
    }

    @Override
    public void notifyCurrentPlayerChanged(String nextPlayer) {
        for (GameObserver o : observers)
            o.onCurrentPlayerChanged(nextPlayer);
    }

    @Override
    public void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        for (GameObserver o : observers)
            o.onPlayerLimitsInitialized(nickname, remainingUpper, remainingLower);
    }

    // ActionResolution
    @Override
    public void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        for (GameObserver o : observers)
            o.onCardTaken(nickname, cardID, cardType, sourceRow);
    }

    @Override
    public void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        for (GameObserver o : observers)
            o.onPlayerLimitsUpdated(nickname, remainingUpper, remainingLower);
    }

    @Override
    public void notifyTotemReturned(String nickname, int turnOrderPosition) {
        for (GameObserver o : observers)
            o.onTotemReturned(nickname, turnOrderPosition);
    }

    @Override
    public void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {
        for (GameObserver o : observers)
            o.onPlayerResourceChanged(nickname, resource, newValue, delta);
    }

    // NewRound
    @Override
    public void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {
        for (GameObserver o : observers)
            o.onBoardUpdated(newUpperRow, newLowerRow, discardedCards, movedToLowerRow, deckRemainingCount);
    }

    @Override
    public void notifyTurnOrderEstablished(List<String> turnOrder) {
        for (GameObserver o : observers)
            o.onTurnOrderEstablished(turnOrder);
    }

    // NewEra
    @Override
    public void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {
        for (GameObserver o : observers)
            o.onEraChanged(newEra, newUpperRowBuildings, newLowerRowBuildings, discardedBuildings);
    }

    // Phase changes
    @Override
    public void notifyPhaseChanged(PhaseType phase) {
        for (GameObserver o : observers)
            o.onPhaseChanged(phase);
    }

    @Override
    public void notifyPhaseChanged(PhaseType phase, String currentPlayer) {
        for (GameObserver o : observers)
            o.onPhaseChanged(phase, currentPlayer);
    }

    @Override
    public void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        for (GameObserver o : observers)
            o.onPhaseChanged(phase, currentPlayer, resolutionOrder);
    }

    // EventResolution
    @Override
    public void notifyEventResolved(String eventID, String eventName) {
        for (GameObserver o : observers)
            o.onEventResolved(eventID, eventName);
    }

    @Override
    public void notifySustainmentResolved(String nickname, int totalCharacters, int foodPaid, int foodShortage, int ppLost) {
        for (GameObserver o : observers)
            o.onSustainmentResolved(nickname, totalCharacters, foodPaid, foodShortage, ppLost);
    }

    // ExtraTurn
    @Override
    public void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        for (GameObserver o : observers)
            o.onExtraTurnStarted(nickname, remainingUpper, remainingLower);
    }

    @Override
    public void notifyExtraTurnEnded(String nickname) {
        for (GameObserver o : observers)
            o.onExtraTurnEnded(nickname);
    }

    // FinaScoring
    @Override
    public void notifyFinalScoreCalculated(String nickname, int ppFromBuilders, int ppFromBuildings, int ppFromInventors, int ppFromArtists, int ppFromBuildingEffects, int totalPP, int remainingFood) {
        for (GameObserver o : observers)
            o.onFinalScoreCalculated(nickname, ppFromBuilders, ppFromBuildings, ppFromInventors, ppFromArtists, ppFromBuildingEffects, totalPP, remainingFood);
    }

    @Override
    public void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        for (GameObserver o : observers)
            o.onGameEnded(winners, finalRankings);
    }
}
