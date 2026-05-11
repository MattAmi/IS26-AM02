package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.gui.scenes.*;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

/**
 * Main Controller for the GUI. Handles SPA navigation, network events,
 * and synchronization between the Model and the View layers.
 */
public class GuiController extends ClientController {

    private final Stage primaryStage;
    private final ClientView clientView;

    // --- SPA ARCHITECTURE LAYERS ---
    private StackPane rootContainer;
    private StackPane baseLayer;    // Background: Current active scene (Menu, Lobby, Game)
    private StackPane modalLayer;   // Middle: Blocking overlays (Popups, Network Form)
    private VBox toastLayer;        // Top: Non-blocking notifications

    // Scene references to manage updates
    private LobbyListScene lobbyListScene;
    private LobbyScene lobbyScene;
    private GameScene gameScene;
    private LobbyModel lobbyModel;

    public GuiController(Stage primaryStage, ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        super(proxy, lobbyModel, view);
        this.primaryStage = primaryStage;
        this.clientView = view;
        this.lobbyModel = lobbyModel;
        initWindowArchitecture();
    }

    /**
     * Initializes the "Layered Sandwich" root container and sets the primary Scene.
     */

    public void initWindowArchitecture() {
        baseLayer = new StackPane();
        baseLayer.setStyle("-fx-background-color: #1a1a1a;");

        modalLayer = new StackPane();
        modalLayer.setStyle("-fx-background-color: transparent;");
        modalLayer.setPickOnBounds(false);
        modalLayer.setVisible(false);
        modalLayer.setAlignment(Pos.CENTER);

        toastLayer = new VBox(10);
        toastLayer.setAlignment(Pos.TOP_RIGHT);
        toastLayer.setPadding(new Insets(20));
        toastLayer.setPickOnBounds(false);

        rootContainer = new StackPane(baseLayer, modalLayer, toastLayer);
        Scene mainScene = new Scene(rootContainer, 400, 450);

        primaryStage.setTitle("Mesos - Board Game");
        primaryStage.setScene(mainScene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void start() {
        primaryStage.show();
        // Entry point: Trigger network connection form inside the modal layer
        promptConnectionAndRetry();
    }

    // --- SPA NAVIGATION METHODS ---

    private void switchView(Region newView) {
        Platform.runLater(() -> baseLayer.getChildren().setAll(newView));
    }

    public void showIntroScene() {
        primaryStage.setResizable(true);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        primaryStage.centerOnScreen();
        Platform.runLater(() -> switchView(new IntroScene().buildNode(this::showGameMenuScene)));
    }

    public void showGameMenuScene() {
        Platform.runLater(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = null;
            switchView(new GameMenuScene().buildNode(this));
        });
    }

    public void showLobbyListScene() {
        Platform.runLater(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = new LobbyListScene();
            switchView(lobbyListScene.buildNode(this));
            handleAvailableLobbiesUpdated(lobbyModel.getAvailableLobbies());
        });
    }

    public void showLobbyScene() {
        Platform.runLater(() -> {
            this.gameScene = null;
            this.lobbyListScene = null;
            this.lobbyScene = new LobbyScene();
            switchView(lobbyScene.buildNode(this));
            if (lobbyModel.getCurrentLobby() != null) {
                lobbyScene.updateLobbyState(lobbyModel.getCurrentLobby());
            }
        });
    }

    public void switchToGameScene(String gameId) {
        Platform.runLater(() -> {
            this.lobbyScene = null;
            this.lobbyListScene = null;
            this.gameScene = new GameScene();
            switchView(gameScene.buildNode(this));
        });
    }

    // --- ACTIONS CALLED BY SCENES (UI -> Controller -> Proxy) ---

    public void requestSetUsername(String nickname) { handleSetNickname(nickname); }

    public void requestCreateLobby(int size) {
        showToast("Processing", "Creating your lobby...", Alert.AlertType.INFORMATION);
        handleCreateLobby(size);
    }

    public void requestJoinLobby(String lobbyId) {
        List<LobbyInfo> lobbies = lobbyModel.getAvailableLobbies();
        for (LobbyInfo l : lobbies) {
            if (l.lobbyId().equals(lobbyId)) {
                handleJoinLobby(lobbies.indexOf(l));
                return;
            }
        }
    }
    public void requestReconnect(String nick, String gId) { proxy.requestReconnect(nick, gId); }
    public void requestSelectTotem(Totem t) { proxy.requestSelectTotem(t); }
    public void requestLeaveLobby() {
        if (lobbyModel.getCurrentLobby() != null) {
            proxy.requestLeaveLobby();
        } else {
            handleReturnToLobby();
        }
    }
    public void moveTotem(char t) { proxy.moveTotem(t); }
    public void resolveActions(List<String> ids) { proxy.resolveActions(ids); }


    // --- MODEL NOTIFICATION HANDLERS (Model -> Controller -> UI) ---

    public void refreshFullGameScene(GameModel gameModel) {
        if (gameScene != null) gameScene.refreshAll(gameModel);
    }

    public void handleUsernameResult(String username, boolean accepted, String reason) {
        Platform.runLater(() -> {
            if (!accepted) {
                showToast("Nickname Rejected", reason, Alert.AlertType.WARNING);
                if (lobbyScene != null) lobbyScene.onNicknameRejected();
            } else {
                if (lobbyScene != null) lobbyScene.onNicknameAccepted(username);
                else showIntroScene();
            }
        });
    }

    public void handleAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        Platform.runLater(() -> {
            if (this.lobbyScene != null || this.gameScene != null) {
                showGameMenuScene();
            }
            if (this.lobbyListScene != null) {
                lobbyListScene.onAvailableLobbiesUpdated(lobbies);
            }
        });
    }

    /**
     * CRITICAL FIX: Transitions to LobbyScene automatically when joined/created.
     */
    public void handleCurrentLobbyUpdated(LobbyInfo lobby) {
        Platform.runLater(() -> {
            if (this.lobbyScene == null) {
                showLobbyScene();
            } else {
                lobbyScene.updateLobbyState(lobby);
            }
        });
    }

    public void handleLobbyDissolved() {
        showBlockingAlert("Lobby Closed", "The lobby has been dissolved.", Alert.AlertType.INFORMATION);
        showGameMenuScene();
    }

    // Standard Game Handlers
    public void handleCurrentPlayerChanged(String n) { refreshFullGameScene(getGameModel()); }
    public void handleTurnOrderEstablished(List<String> t) { refreshFullGameScene(getGameModel()); }
    public void handleTotemPlaced(String n, char t) { refreshFullGameScene(getGameModel()); }
    public void handleTotemReturned(String n, int p) { refreshFullGameScene(getGameModel()); }
    public void handleOfferTilesUpdated(List<OfferTileInfo> o) { refreshFullGameScene(getGameModel()); }
    public void handleBoardUpdated(List<String> u, List<String> l, int d) { refreshFullGameScene(getGameModel()); }
    public void handleEraChanged(List<String> u, List<String> l) { refreshFullGameScene(getGameModel()); }
    public void handlePlayerLimitsUpdated(String n, int u, int l) { refreshFullGameScene(getGameModel()); }
    public void handlePlayerResourceChanged(String n, ResourceType r, int v) { refreshFullGameScene(getGameModel()); }
    public void handleCardTaken(String n, String c, CardType t, RowPosition s) { refreshFullGameScene(getGameModel()); }
    public void handleExtraTurnStarted(String n, int u, int l) { refreshFullGameScene(getGameModel()); }
    public void handleEventResolved(String id, String n) { refreshFullGameScene(getGameModel()); }
    public void handleShowAvailableTotems() { }
    public void handleExtraTurnEnded(String n) { refreshFullGameScene(getGameModel()); }

    // Game Recovery & Termination
    public void handleGameEnded(List<String> w, List<PlayerFinalScore> r) { showBlockingAlert("Game Over", "Winners: " + w, Alert.AlertType.INFORMATION); }
    public void handlePlayerDisconnected(String n) { if (gameScene != null) gameScene.setPlayerOffline(n); }
    public void handleGameAborted(String l) { showBlockingAlert("Aborted", "Game ended. Last standing: " + l, Alert.AlertType.INFORMATION); requestReturnToLobby(); }
    public void handleGameRecoveryFailed() { showBlockingAlert("Error", "Recovery failed", Alert.AlertType.ERROR); requestReturnToLobby(); }
    public void handlePlayerReconnected(String n) { if (gameScene != null) gameScene.setPlayerOnline(n); }

    public void handleError(String message) {
        showToast("Errore", message, Alert.AlertType.WARNING);
        Platform.runLater(() -> {
            if (this.lobbyScene != null) {
                this.lobbyScene.onNicknameRejected();
            }
        });
    }
    public void handleConnectionLost() {
        Platform.runLater(() -> {
            VBox alertBox = new VBox(16);
            alertBox.setAlignment(Pos.CENTER);
            alertBox.setPadding(new Insets(32));
            alertBox.setMaxSize(400, 250);
            alertBox.setStyle(
                    "-fx-background-color: #1C1C1C;" +
                            "-fx-border-color: #C0392B;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 12;" +
                            "-fx-background-radius: 12;"
            );

            Label icon = new Label("⚠");
            icon.setStyle("-fx-font-size: 36; -fx-text-fill: #C0392B;");

            Label title = new Label("Connection to the server has been lost");
            title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");

            Label subtitle = new Label("Attempting to automatically reconnect...");
            subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #AAAAAA;");
            subtitle.setWrapText(true);

            javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
            spinner.setMaxSize(40, 40);
            spinner.setStyle("-fx-accent: #E67E22;");

            alertBox.getChildren().addAll(icon, title, subtitle, spinner);
            modalLayer.getChildren().setAll(alertBox);
            modalLayer.setVisible(true);
        });
    }

    public void handleConnectionRestored() {
        Platform.runLater(() -> {
            modalLayer.setVisible(false);
            showToast("Connected", "You're back online!", Alert.AlertType.INFORMATION);
        });
    }

    /**
     * Initiates the return to lobby sequence by cleaning up state and proxy.
     * This acts like the TUI "lobby" command.
     */
    public void requestReturnToLobby() {
        performReturnToLobby();
    }

    /**
     * Handles the UI transition after state cleanup.
     * This acts like the TUI onReturnToLobby implementation.
     */
    public void handleReturnToLobby() {
        Platform.runLater(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = null;
            showGameMenuScene();
        });
    }

    // --- NATIVE SPA UI COMPONENTS (TOASTS & MODALS) ---

    private void showToast(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            VBox toast = new VBox(5);
            String borderColor = type == Alert.AlertType.ERROR ? "#ff4444" : (type == Alert.AlertType.WARNING ? "#ffbb33" : "#00C851");
            toast.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-padding: 15;");

            Label tL = new Label(title); tL.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            Label cL = new Label(content); cL.setStyle("-fx-text-fill: lightgray;"); cL.setWrapText(true);

            toast.getChildren().addAll(tL, cL);
            toastLayer.getChildren().add(toast);

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> toastLayer.getChildren().remove(toast));
            delay.play();
        });
    }

    private void showBlockingAlert(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            VBox alertBox = new VBox(20);
            alertBox.setAlignment(Pos.CENTER); alertBox.setPadding(new Insets(30)); alertBox.setMaxSize(400, 200);
            alertBox.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

            Label tL = new Label(title); tL.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #F2D5A3;");
            Label cL = new Label(content); cL.setStyle("-fx-text-fill: white;"); cL.setWrapText(true); cL.setAlignment(Pos.CENTER);

            Button okBtn = new Button("OK"); okBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white;");
            okBtn.setOnAction(e -> modalLayer.setVisible(false));

            alertBox.getChildren().addAll(tL, cL, okBtn);
            modalLayer.getChildren().setAll(alertBox);
            modalLayer.setVisible(true);
        });
    }

    public void showSummaryCard() {
        Platform.runLater(() -> {
            SummaryCardOverlay overlay = new SummaryCardOverlay();
            VBox node = overlay.buildNode(() -> {
                modalLayer.setVisible(false);
                modalLayer.getChildren().clear();
            });
            modalLayer.getChildren().setAll(node);
            modalLayer.setVisible(true);
        });
    }

    public void showInGameMenu() {
        Platform.runLater(() -> {
            InGameMenuOverlay overlay = new InGameMenuOverlay();
            VBox node = overlay.buildNode(this, () -> {
                modalLayer.setVisible(false);
                modalLayer.getChildren().clear();
            }, modalLayer);
            modalLayer.getChildren().setAll(node);
            modalLayer.setVisible(true);
        });
    }

    public void promptConnectionAndRetry() {
        Platform.runLater(() -> {
            StackPane connectionForm = NetworkPopup.buildNode((config, onError) -> {
                Thread connectionThread = new Thread(() -> {
                    try {
                        ServerProxy newProxy = ServerProxyFactory.create(
                                config.type(), config.host(), config.port(), lobbyModel, clientView);

                        this.setServerProxy(newProxy);
                        newProxy.setClientController(this);
                        newProxy.connect();

                        Platform.runLater(() -> {
                            modalLayer.setVisible(false);
                            showToast("Connected", "Successfully connected via " + config.type(), Alert.AlertType.INFORMATION);
                            showIntroScene();
                        });
                    } catch (Exception ex) {
                        onError.accept(ex.getMessage());
                    }
                });
                connectionThread.setDaemon(true);
                connectionThread.start();
            });

            modalLayer.getChildren().setAll(connectionForm);
            modalLayer.setVisible(true);
        });
    }

    /**
     * Sincronizza il passaggio alla scena di gioco se non ci siamo ancora.
     */
    private void ensureInGameScene() {
        Platform.runLater(() -> {
            if (this.gameScene == null) {
                // Se siamo qui, significa che siamo appena rientrati (reconnect)
                // o la partita è appena iniziata.
                modalLayer.setVisible(false); // Chiudiamo eventuali form rimasti aperti
                switchToGameScene(getGameModel().getGameId());
            }
        });
    }

    // --- MODIFICA QUESTI HANDLER ---

    public void handleGameSetupCompleted(List<String> t, Map<String, Integer> f, BoardSnapshot b) {
        ensureInGameScene(); // Assicurati di cambiare scena prima di refreshare
        refreshFullGameScene(getGameModel());
    }

    public void handlePlayerLimitsInitialized(String n, int u, int l) {
        // Spesso è il primo evento di sync che arriva dopo la riconnessione
        ensureInGameScene();
        refreshFullGameScene(getGameModel());
    }

    public void handlePhaseChanged(PhaseType p, String c, List<String> r) {
        ensureInGameScene();
        refreshFullGameScene(getGameModel());
    }
}