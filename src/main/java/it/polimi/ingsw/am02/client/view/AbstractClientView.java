package it.polimi.ingsw.am02.client.view;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;

import java.util.List;
import java.util.Map;

/**
 * Default no-op implementation of {@link ClientView}.
 * Concrete views (TUI, GUI) extend this class and override only
 * the methods they actually need to handle.
 */
public abstract class AbstractClientView implements ClientView {

    @Override public void onUsernameResult(String username, boolean accepted, String reason) {}
    @Override public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {}
    @Override public void onCurrentLobbyUpdated(LobbyInfo lobby) {}
    @Override public void onShowAvailableTotems() {}
    @Override public void onLobbyDissolved() {}

    @Override public void onGameStarted(String gameId) {}
    @Override public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {}
    @Override public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {}
    @Override public void onCurrentPlayerChanged(String nextPlayer) {}
    @Override public void onTurnOrderEstablished(List<String> turnOrder) {}

    @Override public void onTotemPlaced(String nickname, char tileID) {}
    @Override public void onTotemReturned(String nickname, int turnOrderPosition) {}
    @Override public void onOfferTilesUpdated(List<OfferTileInfo> offerTiles) {}
    @Override public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) {}
    @Override public void onEraChanged(List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {}

    @Override public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {}
    @Override public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {}
    @Override public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) {}
    @Override public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {}

    @Override public void onEventResolved(String EventID, String eventName) {}
    @Override public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {}
    @Override public void onExtraTurnEnded(String nickname) {}

    @Override public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {}
    @Override public void onPlayerDisconnected(String nickname) {}

    @Override public void onGameAborted(String lastManStanding) {}
    @Override public void onGameRecoveryFailed() {}
    @Override public void onPlayerReconnected(String nickname) {}

    @Override
    public void onError(String message) {
        System.err.println("[ERROR] " + message);
    }
    @Override public void setGameModel(GameModel gameModel) {}
    @Override public void onConnectionLost() {}
    @Override public void onConnectionRestored() {}
    @Override public void onAutoPlayerTimerStarted(String nickname) {}
    @Override public void onAutoPlayerInvoked(String nickname) {}

    @Override public void onReturnToLobby() {}
}