package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.List;
import java.util.Map;

public interface GameObserver {
    void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot);

    void onPhaseChanged(PhaseType phase);
    void onPhaseChanged(PhaseType phase, String currentPlayer);
    void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);

    void onCurrentPlayerChanged(String nextPlayer);
    void onTurnOrderEstablished(List<String> turnOrder);
    void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings);

    void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount);
    void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);

    void onTotemPlaced(String nickname, char tileID);
    void onTotemReturned(String nickname, int turnOrderPosition);

    void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);
    void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);
    void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta);

    void onEventResolved(String eventID, String eventName);

    void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);
    void onExtraTurnEnded(String nickname);

    void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);
}