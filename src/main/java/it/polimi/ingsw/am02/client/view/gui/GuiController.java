package it.polimi.ingsw.am02.client.view.gui;
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

public class GuiController {
    public void refreshFullGameScene(GameModel gameModel) {
    }

    public void handleUsernameResult(String username, boolean accepted, String reason) {
    }

    public void handleAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
    }

    public void handleCurrentLobbyUpdated(LobbyInfo lobby) {
    }

    public void handleLobbyDissolved() {
    }

    public void switchToGameScene(String gameId) {
    }

    public void handleGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {
    }

    public void handlePhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
    }

    public void handleCurrentPlayerChanged(String nextPlayer) {
    }

    public void handleTurnOrderEstablished(List<String> turnOrder) {
    }

    public void handleTotemPlaced(String nickname, char tileID) {
    }

    public void handleTotemReturned(String nickname, int turnOrderPosition) {
    }

    public void handleOfferTilesUpdated(List<OfferTileInfo> offerTiles) {
    }

    public void handleBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) {
    }

    public void handleEraChanged(List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {
    }

    public void handlePlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
    }

    public void handlePlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
    }

    public void handlePlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
    }

    public void handleCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
    }

    public void handleEventResolved(String eventID, String eventName) {
    }

    public void handleExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
    }

    public void handleExtraTurnEnded(String nickname) {
    }

    public void handleGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
    }

    public void handlePlayerDisconnected(String nickname) {
    }

    public void handleGameAborted(String lastManStanding) {
    }

    public void handleGameRecoveryFailed() {
    }

    public void handlePlayerReconnected(String nickname) {
    }

    public void handleError(String message) {
    }

    public void handleShowAvailableTotems() {
    }

    public void handleConnectionLost() {
    }

    public void handleConnectionRestored() {
    }

    public void handleReturnToLobby() {
    }
}
