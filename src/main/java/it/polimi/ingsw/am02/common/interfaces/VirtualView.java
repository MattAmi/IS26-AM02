import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.List;
import java.util.Map;

public interface VirtualView {

    /** Silent no-operation implementation used during server-side replay. */
    final class NoOp implements VirtualView {
        private NoOp() {}
        @Override public void notifyGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {}
        @Override public void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {}
        @Override public void notifyCurrentPlayerChanged(String nextPlayer) {}
        @Override public void notifyTurnOrderEstablished(List<String> turnOrder) {}
        @Override public void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {}
        @Override public void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {}
        @Override public void notifyTotemPlaced(String nickname, char tileID) {}
        @Override public void notifyTotemReturned(String nickname, int turnOrderPosition) {}
        @Override public void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {}
        @Override public void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {}
        @Override public void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {}
        @Override public void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {}
        @Override public void notifyEventResolved(String eventID, String eventName) {}
        @Override public void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {}
        @Override public void notifyExtraTurnEnded(String nickname) {}
        @Override public void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {}
        @Override public void notifyError(String message) {}
        @Override public void notifyPlayerDisconnected(String nickname) {}
        @Override public void notifyPlayerReconnected(String nickname) {}
        @Override public void notifyAutoPlayerTimerStarted(String nickname) {}
        @Override public void notifyAutoPlayerInvoked(String nickname) {}
        @Override public void notifyGameAborted(String winner) {}
        @Override public void notifyGameRecoveryFailed() {}
    }

    VirtualView NO_OP = new NoOp();
    static VirtualView noOp() { return NO_OP; }

    // Lifecycle
    void notifyGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot);
    void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);
    void notifyCurrentPlayerChanged(String nextPlayer);
    void notifyTurnOrderEstablished(List<String> turnOrder);

    // Board
    void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount);
    void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings);

    // Player actions
    void notifyTotemPlaced(String nickname, char tileID);
    void notifyTotemReturned(String nickname, int turnOrderPosition);
    void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);
    void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);
    void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);
    void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta);

    // Events / turns
    void notifyEventResolved(String eventID, String eventName);
    void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);
    void notifyExtraTurnEnded(String nickname);

    // Game end
    void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);

    // Connection (transient)
    void notifyError(String message);
    void notifyPlayerDisconnected(String nickname);
    void notifyPlayerReconnected(String nickname);
    void notifyAutoPlayerTimerStarted(String nickname);
    void notifyAutoPlayerInvoked(String nickname);
    void notifyGameAborted(String winner);
    void notifyGameRecoveryFailed();
}
}