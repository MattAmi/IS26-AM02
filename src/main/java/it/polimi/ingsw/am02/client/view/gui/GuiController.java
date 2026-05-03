package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.gui.scenes.*;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class GuiController {

    private final Stage primaryStage;
    private ServerProxy proxy;

    private GameModel gameModel; // Null until game starts

    private LobbyListScene lobbyListScene;
    // private GameScene gameScene;

    public GuiController(Stage primaryStage, ServerProxy proxy) {
        this.primaryStage = primaryStage;
        this.proxy = proxy;
    }

    // GESTIONE CAMBI SCENA

    public void start() {
        LoginScene loginScene = new LoginScene();
        primaryStage.setScene(loginScene.buildScene(this::showIntroScene));
        primaryStage.setTitle("Mesos");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void showIntroScene() {
        IntroScene introScene = new IntroScene();
        primaryStage.setScene(introScene.buildScene(this::showLobbyListScene));
    }

    public void showLobbyListScene() {
        lobbyListScene = new LobbyListScene();
        primaryStage.setScene(lobbyListScene.buildScene());
    }

    public void switchToGameScene(String gameId) {
        // gameScene = new GameScene();
        // primaryStage.setScene(gameScene.buildScene());
    }

    public void requestSetUsername(String nickname) {
        proxy.requestSetUsername(nickname);
    }

    public void requestCreateLobby(int size) {
        proxy.requestCreateLobby(size);
    }

    public void requestJoinLobby(String lobbyId) {
        proxy.requestJoinLobby(lobbyId);
    }

    public void requestReconnect(String nickname, String gameId) {
        proxy.requestReconnect(nickname, gameId);
    }

    public void requestSelectTotem(Totem selected) {
        proxy.requestSelectTotem(selected);
    }

    public void requestLeaveLobby() {
        proxy.requestLeaveLobby();
    }

    public void moveTotem(char tile) {
        proxy.moveTotem(tile);
    }

    public void resolveActions(List<String> ids) {
        proxy.resolveActions(ids);
    }

    public void disconnect() {
        proxy.disconnect();
    }

    public void returnToMainMenu() {
        if (gameModel != null) {
            if (!gameModel.isGameEnded()) {
                handleError("You cannot return to lobby while a game is in progress.");
            } else {
                try {
                    proxy.disconnect();
                    proxy.connect();
                    this.gameModel = null;
                    handleReturnToLobby();
                } catch (Exception e) {
                    handleError("Return to lobby failed: Server is unreachable.");
                }
            }
        } else {
            // Se eravamo in una lobby, la abbandoniamo. Altrimenti cambiamo solo scena.
            proxy.requestLeaveLobby();
            handleReturnToLobby();
        }
    }

    // GESTIONE EVENTI: DA SERVER A GUI

    public void refreshFullGameScene(GameModel gameModel) {
        this.gameModel = gameModel;
        // if (gameScene != null) gameScene.refreshAll(gameModel);
    }

    public void handleUsernameResult(String username, boolean accepted, String reason) {
        if (!accepted) {
            showAlert("Login Failed", reason, Alert.AlertType.ERROR);
        } else {
            showIntroScene(); // Passaggio automatico se accettato
        }
    }

    public void handleAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        if (lobbyListScene != null) {
            lobbyListScene.onAvailableLobbiesUpdated(lobbies);
        }
    }

    public void handleCurrentLobbyUpdated(LobbyInfo lobby) {
        // if (lobbyListScene != null) lobbyListScene.updateCurrentLobby(lobby);
    }

    public void handleLobbyDissolved() {
        showAlert("Lobby Dissolved", "The lobby has been dissolved.", Alert.AlertType.INFORMATION);
        showLobbyListScene();
    }

    public void handleGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {
        // if (gameScene != null) gameScene.setupInitialBoard(turnOrder, initialFood, board);
    }

    public void handlePhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        // if (gameScene != null) gameScene.updatePhase(phase, currentPlayer, resolutionOrder);
    }

    public void handleCurrentPlayerChanged(String nextPlayer) {
        // if (gameScene != null) gameScene.highlightCurrentPlayer(nextPlayer);
    }

    public void handleTurnOrderEstablished(List<String> turnOrder) {
        // if (gameScene != null) gameScene.updateTurnOrder(turnOrder);
    }

    public void handleTotemPlaced(String nickname, char tileID) {
        // if (gameScene != null) gameScene.animateTotemPlacement(nickname, tileID);
    }

    public void handleTotemReturned(String nickname, int turnOrderPosition) {
        // if (gameScene != null) gameScene.animateTotemReturn(nickname, turnOrderPosition);
    }

    public void handleOfferTilesUpdated(List<OfferTileInfo> offerTiles) {
        // if (gameScene != null) gameScene.updateOfferTiles(offerTiles);
    }

    public void handleBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) {
        // if (gameScene != null) gameScene.updateBoardCards(newUpperRow, newLowerRow, deckRemainingCount);
    }

    public void handleEraChanged(List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {
        // if (gameScene != null) gameScene.showNewEraAnimation(newUpperRowBuildings, newLowerRowBuildings);
    }

    public void handlePlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        // if (gameScene != null) gameScene.updatePlayerLimits(nickname, remainingUpper, remainingLower);
    }

    public void handlePlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        // if (gameScene != null) gameScene.updatePlayerLimits(nickname, remainingUpper, remainingLower);
    }

    public void handlePlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
        // if (gameScene != null) gameScene.updatePlayerResource(nickname, resource, newValue);
    }

    public void handleCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        // if (gameScene != null) gameScene.animateCardTaken(nickname, cardID, cardType, sourceRow);
    }

    public void handleEventResolved(String eventID, String eventName) {
        // if (gameScene != null) gameScene.showEventResolved(eventID, eventName);
    }

    public void handleExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        // if (gameScene != null) gameScene.showExtraTurnAlert(nickname, remainingUpper, remainingLower);
    }

    public void handleExtraTurnEnded(String nickname) {
        // if (gameScene != null) gameScene.endExtraTurn(nickname);
    }

    public void handleGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        // showGameOverScene(winners, finalRankings);
    }

    public void handlePlayerDisconnected(String nickname) {
        // if (gameScene != null) gameScene.setPlayerOffline(nickname);
    }

    public void handleGameAborted(String lastManStanding) {
        showAlert("Game Aborted", "Game ended. Last man standing: " + lastManStanding, Alert.AlertType.INFORMATION);
        handleReturnToLobby();
    }

    public void handleGameRecoveryFailed() {
        showAlert("Recovery Failed", "Could not recover the game state.", Alert.AlertType.ERROR);
        handleReturnToLobby();
    }

    public void handlePlayerReconnected(String nickname) {
        // if (gameScene != null) gameScene.setPlayerOnline(nickname);
    }

    public void handleError(String message) {
        showAlert("Error", message, Alert.AlertType.ERROR);
    }

    public void handleShowAvailableTotems() {
        // if (lobbyListScene != null) lobbyListScene.showTotemSelectionPopup();
    }

    public void handleConnectionLost() {
        showAlert("Connection Lost", "Attempting to reconnect...", Alert.AlertType.WARNING);
    }

    public void handleConnectionRestored() {
        showAlert("Connection Restored", "Successfully reconnected to the server.", Alert.AlertType.INFORMATION);
    }

    public void handleReturnToLobby() {
        this.gameModel = null;
        // gameScene = null;
        showLobbyListScene();
    }

    // =======================================================================
    // UTILITY LOGICHE
    // =======================================================================

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setServerProxy(ServerProxy proxy) {
        this.proxy = proxy;
    }

}