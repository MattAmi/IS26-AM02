package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.AbstractClientView;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;

public class GuiView extends AbstractClientView {

    private GuiController guiController;
    private final LobbyModel lobbyModel;
    private GameModel gameModel;

    public GuiView(GuiController guiController, LobbyModel lobbyModel) {
        this.guiController = guiController;
        this.lobbyModel = lobbyModel;
        this.lobbyModel.addObserver(this);
    }

    public void setGuiController(GuiController guiController) {
        this.guiController = guiController;
    }

    @Override
    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
        if (this.gameModel != null) {
            this.gameModel.addObserver(this);
            Platform.runLater(() -> guiController.refreshFullGameScene(gameModel));
        }
    }

    @Override
    public void onUsernameResult(String username, boolean accepted, String reason) {
        Platform.runLater(() -> guiController.handleUsernameResult(username, accepted, reason));
    }

    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        Platform.runLater(() -> guiController.handleAvailableLobbiesUpdated(lobbies));
    }

    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        Platform.runLater(() -> guiController.handleCurrentLobbyUpdated(lobby));
    }

    @Override
    public void onLobbyDissolved() {
        Platform.runLater(guiController::handleLobbyDissolved);
    }

    @Override
    public void onGameStarted(String gameId) {
        lobbyModel.removeObserver(this);
        Platform.runLater(() -> guiController.switchToGameScene(gameId));
    }

    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {
        Platform.runLater(() -> guiController.handleGameSetupCompleted(turnOrder, initialFood, board));
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        Platform.runLater(() -> guiController.handlePhaseChanged(phase, currentPlayer, resolutionOrder));
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        Platform.runLater(() -> guiController.handleCurrentPlayerChanged(nextPlayer));
    }

    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) {
        Platform.runLater(() -> guiController.handleTurnOrderEstablished(turnOrder));
    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        Platform.runLater(() -> guiController.handleTotemPlaced(nickname, tileID));
    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        Platform.runLater(() -> guiController.handleTotemReturned(nickname, turnOrderPosition));
    }

    @Override
    public void onOfferTilesUpdated(List<OfferTileInfo> offerTiles) {
        Platform.runLater(() -> guiController.handleOfferTilesUpdated(offerTiles));
    }

    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) {
        Platform.runLater(() -> guiController.handleBoardUpdated(newUpperRow, newLowerRow, deckRemainingCount));
    }

    @Override
    public void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {
        //DA CAMBIARE: ho infatti inserito newEra come parametro di input
        Platform.runLater(() -> guiController.handleEraChanged(newUpperRowBuildings, newLowerRowBuildings));
    }

    @Override
    public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        Platform.runLater(() -> guiController.handlePlayerLimitsInitialized(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        Platform.runLater(() -> guiController.handlePlayerLimitsUpdated(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
        Platform.runLater(() -> guiController.handlePlayerResourceChanged(nickname, resource, newValue));
    }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        Platform.runLater(() -> guiController.handleCardTaken(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public void onEventResolved(String eventID, String eventName) {
        Platform.runLater(() -> guiController.handleEventResolved(eventID, eventName));
    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        Platform.runLater(() -> guiController.handleExtraTurnStarted(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onExtraTurnEnded(String nickname) {
        Platform.runLater(() -> guiController.handleExtraTurnEnded(nickname));
    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        Platform.runLater(() -> guiController.handleGameEnded(winners, finalRankings));
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        Platform.runLater(() -> guiController.handlePlayerDisconnected(nickname));
    }

    @Override
    public void onGameAborted(String lastManStanding) {
        Platform.runLater(() -> guiController.handleGameAborted(lastManStanding));
    }

    @Override
    public void onGameRecoveryFailed() {
        Platform.runLater(guiController::handleGameRecoveryFailed);
    }

    @Override
    public void onPlayerReconnected(String nickname) {
        Platform.runLater(() -> guiController.handlePlayerReconnected(nickname));
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> guiController.handleError(message));
    }

    @Override
    public void onShowAvailableTotems() {
        Platform.runLater(guiController::handleShowAvailableTotems);
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(guiController::handleConnectionLost);
    }

    @Override
    public void onConnectionRestored() {
        Platform.runLater(guiController::handleConnectionRestored);
    }

    @Override
    public void onReturnToLobby() {
        Platform.runLater(guiController::handleReturnToLobby);
    }
}
