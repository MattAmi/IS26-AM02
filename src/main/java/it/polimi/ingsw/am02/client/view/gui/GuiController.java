package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.gui.scenes.*;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class GuiController extends ClientController {

    private final Stage primaryStage;
    private LobbyListScene lobbyListScene;
    private LobbyScene lobbyScene;
    private GameScene gameScene;

    public GuiController(Stage primaryStage, ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        super(proxy, lobbyModel, view);
        this.primaryStage = primaryStage;
    }

    public void start() {
        LoginScene loginScene = new LoginScene();
        primaryStage.setTitle("Mesos");
        primaryStage.setResizable(true);
        primaryStage.setScene(loginScene.buildScene(this::showIntroScene));
        primaryStage.show();
    }

    public void showIntroScene() {
        Platform.runLater(() -> {
            IntroScene introScene = new IntroScene();
            primaryStage.setScene(introScene.buildScene(this::showLobbyListScene));
        });
    }

    public void showLobbyListScene() {
        Platform.runLater(() -> {
            this.lobbyScene = null;
            this.lobbyListScene = new LobbyListScene();
            primaryStage.setScene(lobbyListScene.buildScene(this));
            handleAvailableLobbiesUpdated(lobbyModel.getAvailableLobbies());
        });
    }

    public void showLobbyScene() {
        Platform.runLater(() -> {
            this.lobbyScene = new LobbyScene();
            primaryStage.setScene(lobbyScene.buildScene(this));
            if (lobbyModel.getCurrentLobby() != null) {
                lobbyScene.updateLobbyState(lobbyModel.getCurrentLobby());
            }
        });
    }

    public void switchToGameScene(String gameId) {
        Platform.runLater(() -> {
            this.gameScene = new GameScene();
            primaryStage.setScene(gameScene.buildScene(this));
        });
    }

    // ACTIONS CALLED BY SCENES (Delegates to ClientController base methods)

    public void requestSetUsername(String nickname) { handleSetNickname(nickname); }
    public void requestCreateLobby(int size) { handleCreateLobby(size); }
    public void requestJoinLobby(String lobbyId) { 
        List<LobbyInfo> lobbies = lobbyModel.getAvailableLobbies();
        for(int i=0; i<lobbies.size(); i++) {
            if(lobbies.get(i).lobbyId().equals(lobbyId)) {
                handleJoinLobby(i);
                return;
            }
        }
    }
    public void requestReconnect(String nick, String gId) { handleReconnect(nick, gId); }
    public void requestSelectTotem(Totem t) { handleSelectTotem(t); }
    public void requestLeaveLobby() { proxy.requestLeaveLobby(); handleReturnToLobby(); }
    public void moveTotem(char t) { handleMoveTotem(t); }
    public void resolveActions(List<String> ids) { handleResolveActions(ids); }

    // NOTIFICATIONS FROM MODEL (Called by GuiView)

    public void refreshFullGameScene(GameModel gameModel) {
        if (gameScene != null) gameScene.refreshAll(gameModel);
    }

    public void handleUsernameResult(String username, boolean accepted, String reason) {
        Platform.runLater(() -> {
            if (!accepted) {
                showAlert("Nickname Rejected", reason, Alert.AlertType.WARNING);
                if (lobbyScene != null) lobbyScene.onNicknameRejected();
            } else {
                if (lobbyScene != null) {
                    lobbyScene.onNicknameAccepted(username);
                } else {
                    showIntroScene();
                }
            }
        });
    }

    public void handleAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        Platform.runLater(() -> {
            if (lobbyListScene != null) {
                lobbyListScene.onAvailableLobbiesUpdated(lobbies);
            }
        });
    }

    public void handleCurrentLobbyUpdated(LobbyInfo lobby) {
        Platform.runLater(() -> {
            if (lobbyScene != null) {
                lobbyScene.updateLobbyState(lobby);
            }
        });
    }

    public void handleLobbyDissolved() {
        Platform.runLater(() -> {
            showAlert("Lobby Closed", "The lobby has been dissolved.", Alert.AlertType.INFORMATION);
            showLobbyListScene();
        });
    }

    public void handleGameSetupCompleted(List<String> t, Map<String, Integer> f, BoardSnapshot b) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handlePhaseChanged(PhaseType p, String c, List<String> r) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleCurrentPlayerChanged(String n) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleTurnOrderEstablished(List<String> t) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleTotemPlaced(String n, char t) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleTotemReturned(String n, int p) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleOfferTilesUpdated(List<OfferTileInfo> o) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleBoardUpdated(List<String> u, List<String> l, int d) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleEraChanged(List<String> u, List<String> l) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handlePlayerLimitsInitialized(String n, int u, int l) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handlePlayerLimitsUpdated(String n, int u, int l) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handlePlayerResourceChanged(String n, ResourceType r, int v) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleCardTaken(String n, String c, CardType t, RowPosition s) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleShowAvailableTotems() { }
    public void handleEventResolved(String i, String n) { }
    public void handleExtraTurnStarted(String n, int u, int l) { }
    public void handleExtraTurnEnded(String n) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleGameEnded(List<String> w, List<PlayerFinalScore> r) { showAlert("Game Over", "Winners: " + w, Alert.AlertType.INFORMATION); }
    public void handlePlayerDisconnected(String n) { if (gameScene != null) gameScene.setPlayerOffline(n); }
    public void handleGameAborted(String l) { showAlert("Aborted", "Last man standing: " + l, Alert.AlertType.INFORMATION); handleReturnToLobby(); }
    public void handleGameRecoveryFailed() { showAlert("Error", "Recovery failed", Alert.AlertType.ERROR); handleReturnToLobby(); }
    public void handlePlayerReconnected(String n) { if (gameScene != null) gameScene.setPlayerOnline(n); }
    public void handleError(String message) { Platform.runLater(() -> showAlert("Error", message, Alert.AlertType.ERROR)); }
    public void handleConnectionLost() { Platform.runLater(() -> showAlert("Connection Lost", "Server unreachable", Alert.AlertType.ERROR)); }
    public void handleConnectionRestored() { Platform.runLater(() -> showAlert("Connected", "Back online", Alert.AlertType.INFORMATION)); }

    public void handleReturnToLobby() {
        Platform.runLater(() -> {
            performReturnToLobby();
            this.gameScene = null;
            this.lobbyScene = null;
            showLobbyListScene();
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
