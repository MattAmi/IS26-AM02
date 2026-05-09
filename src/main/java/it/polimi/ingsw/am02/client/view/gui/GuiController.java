package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.gui.scenes.*;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

public class GuiController extends ClientController {

    private final Stage primaryStage;
    private final ClientView clientView; // Kept locally to avoid access scope issues with superclass

    private LobbyListScene lobbyListScene;
    private LobbyScene lobbyScene;
    private GameScene gameScene;

    public GuiController(Stage primaryStage, ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        super(proxy, lobbyModel, view);
        this.primaryStage = primaryStage;
        this.clientView = view;
    }

    public void start() {
        LoginScene loginScene = new LoginScene();
        primaryStage.setTitle("Mesos - Board Game");
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

    // --- ACTIONS CALLED BY SCENES (Delegates to Network Proxy) ---

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

    // --- NOTIFICATIONS FROM MODEL (Called by GuiView) ---

    public void refreshFullGameScene(GameModel gameModel) {
        if (gameScene != null) gameScene.refreshAll(gameModel);
    }

    public void handleUsernameResult(String username, boolean accepted, String reason) {
        Platform.runLater(() -> {
            if (!accepted) {
                showToast("Nickname Rejected", reason, Alert.AlertType.WARNING);
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
            if (lobbyListScene != null) lobbyListScene.onAvailableLobbiesUpdated(lobbies);
        });
    }

    public void handleCurrentLobbyUpdated(LobbyInfo lobby) {
        Platform.runLater(() -> {
            if (lobbyScene != null) lobbyScene.updateLobbyState(lobby);
        });
    }

    public void handleLobbyDissolved() {
        Platform.runLater(() -> {
            showBlockingAlert("Lobby Closed", "The lobby has been dissolved.", Alert.AlertType.INFORMATION);
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

    // Additional Handlers
    public void handleExtraTurnStarted(String n, int u, int l) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleEventResolved(String id, String n) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }
    public void handleShowAvailableTotems() { }
    public void handleExtraTurnEnded(String n) { if (gameScene != null) gameScene.refreshAll(getGameModel()); }

    public void handleGameEnded(List<String> w, List<PlayerFinalScore> r) { showBlockingAlert("Game Over", "Winners: " + w, Alert.AlertType.INFORMATION); }
    public void handlePlayerDisconnected(String n) { if (gameScene != null) gameScene.setPlayerOffline(n); }
    public void handleGameAborted(String l) { showBlockingAlert("Aborted", "Last man standing: " + l, Alert.AlertType.INFORMATION); handleReturnToLobby(); }
    public void handleGameRecoveryFailed() { showBlockingAlert("Error", "Recovery failed", Alert.AlertType.ERROR); handleReturnToLobby(); }
    public void handlePlayerReconnected(String n) { if (gameScene != null) gameScene.setPlayerOnline(n); }

    public void handleError(String message) { Platform.runLater(() -> showToast("Error", message, Alert.AlertType.WARNING)); }
    public void handleConnectionLost() { Platform.runLater(() -> showToast("Connection Lost", "Server unreachable, attempting recovery...", Alert.AlertType.ERROR)); }
    public void handleConnectionRestored() { Platform.runLater(() -> showToast("Connected", "Back online!", Alert.AlertType.INFORMATION)); }

    public void handleReturnToLobby() {
        Platform.runLater(() -> {
            performReturnToLobby();
            this.gameScene = null;
            this.lobbyScene = null;
            showLobbyListScene();
        });
    }

    // --- UI UTILITIES & NETWORK RECOVERY ---

    /**
     * Non-blocking toast notification. Does not freeze the game.
     */
    private void showToast(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show();

            PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
            delay.setOnFinished(e -> alert.close());
            delay.play();
        });
    }

    /**
     * Blocking alert used only for critical stops (like Game Over).
     */
    private void showBlockingAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Opens the Network Popup to retry connection when the server is unreachable.
     */
    public void promptConnectionAndRetry() {
        Platform.runLater(() -> {
            // 1. Show blocking error
            showBlockingAlert("Connection Failed",
                    "Unable to reach the server. Please verify the IP, Port, and ensure the ServerApp is running.",
                    Alert.AlertType.ERROR);

            // 2. Open the Network Configuration Popup
            NetworkPopup.ConnectionConfig newConfig = NetworkPopup.displayAndChoose();

            if (newConfig != null) {
                try {
                    // 3. Recreate the Proxy with new parameters
                    ServerProxy newProxy = ServerProxyFactory.create(
                            newConfig.type(),
                            newConfig.host(),
                            newConfig.port(),
                            lobbyModel,
                            clientView // Safe local reference
                    );

                    // 4. Update References
                    this.setServerProxy(newProxy);
                    newProxy.setClientController(this);

                    // 5. Retry Connection in Background
                    Thread retryThread = new Thread(() -> {
                        try {
                            newProxy.connect();
                            System.out.println("[GUI] Connection successfully established on retry.");
                            Platform.runLater(() -> showToast("Connected", "Connection successfully established!", Alert.AlertType.INFORMATION));
                        } catch (Exception e) {
                            System.err.println("[GUI] Reconnection failed: " + e.getMessage());
                            // Recursive call to prompt again if it fails
                            promptConnectionAndRetry();
                        }
                    });
                    retryThread.setDaemon(true);
                    retryThread.start();

                } catch (Exception ex) {
                    showBlockingAlert("System Error", "Unable to create the network proxy.", Alert.AlertType.ERROR);
                }
            } else {
                // Exit game if user closes the popup
                System.exit(0);
            }
        });
    }
}