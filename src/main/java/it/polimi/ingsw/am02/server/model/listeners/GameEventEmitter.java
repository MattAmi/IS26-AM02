package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.List;
import java.util.Map;

public interface GameEventEmitter {
    // SetUp
    void notifyGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot);

    // TotemPlacement
    void notifyTotemPlaced(String nickname, char tileID);
    void notifyCurrentPlayerChanged(String nextPlayer);
    void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);

    // ActionResolution
    void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);
    void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);
    void notifyTotemReturned(String nickname, int turnOrderPosition);
    void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta);

    // NewRound
    void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount);
    void notifyTurnOrderEstablished(List<String> turnOrder);

    // NewEra
    void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings);

    // Phase changes
    void notifyPhaseChanged(PhaseType phase);
    void notifyPhaseChanged(PhaseType phase, String currentPlayer);
    void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);

    // EventResolution
    void notifyEventResolved(String eventID, String eventName);
    void notifySustainmentResolved(String nickname, int totalCharacters, int foodPaid, int foodShortage, int ppLost);

    // ExtraTurn
    void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);
    void notifyExtraTurnEnded(String nickname);

    // FinaScoring
    void notifyFinalScoreCalculated(String nickname, int ppFromBuilders, int ppFromBuildings, int ppFromInventors, int ppFromArtists, int ppFromBuildingEffects, int totalPP, int remainingFood);
    void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);
}
