package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.gui.scenes.*;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class SceneRouter {

    private final Stage primaryStage;
    private GuiController guiController;

    private StackPane rootContainer;
    private StackPane baseLayer;
    private StackPane modalLayer;
    private VBox toastLayer;

    private LobbyListScene lobbyListScene;
    private LobbyScene lobbyScene;
    private GameScene gameScene;

    public SceneRouter(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initWindowArchitecture();
    }

    public void setGuiController(GuiController guiController) {
        this.guiController = guiController;
    }

    private void initWindowArchitecture() {
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
        primaryStage.setFullScreenExitHint("Press ESC to exit full screen");
    }

    // GESTORE THREAD-SAFE UNIVERSALE
    private void runOnUi(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public void show() {
        runOnUi(primaryStage::show);
    }

    private void switchView(Region newView) {
        runOnUi(() -> baseLayer.getChildren().setAll(newView));
    }

    public void showProjectInfoScene(Runnable onExit) {
        runOnUi(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = null;
            ProjectInfoScene infoScene = new ProjectInfoScene();
            switchView(infoScene.buildNode(onExit));
        });
    }

    public void showIntroScene() {
        runOnUi(() -> {
            primaryStage.setResizable(true);
            primaryStage.setFullScreen(true);
            primaryStage.centerOnScreen();
            switchView(new IntroScene().buildNode(this::showGameMenuScene));
        });
    }

    public void showGameMenuScene() {
        runOnUi(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = null;
            switchView(new GameMenuScene().buildNode(
                    guiController,
                    () -> showLobbyListScene(guiController.getAvailableLobbies()),
                    msg -> showToast("Error", msg, Alert.AlertType.WARNING)
            ));
        });
    }

    public void showLobbyListScene(List<LobbyInfo> lobbies) {
        runOnUi(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = new LobbyListScene();
            switchView(lobbyListScene.buildNode(guiController, this::showGameMenuScene));
            if (lobbies != null) lobbyListScene.onAvailableLobbiesUpdated(lobbies);
        });
    }

    public void showLobbyScene(LobbyInfo currentLobby) {
        runOnUi(() -> {
            this.gameScene = null;
            this.lobbyListScene = null;
            this.lobbyScene = new LobbyScene();
            switchView(lobbyScene.buildNode(guiController));
            if (currentLobby != null) lobbyScene.updateLobbyState(currentLobby);
        });
    }

    public void switchToGameScene(GameModel gameModel) {
        runOnUi(() -> {
            this.lobbyScene = null;
            this.lobbyListScene = null;
            this.gameScene = new GameScene();
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            switchView(this.gameScene.buildNode(guiController, this::showInGameMenu, this::showSummaryCard));
            if (gameModel != null) this.gameScene.refreshAll(gameModel);
        });
    }

    public LobbyListScene getLobbyListScene() { return lobbyListScene; }
    public LobbyScene getLobbyScene() { return lobbyScene; }
    public GameScene getGameScene() { return gameScene; }

    public void showToast(String title, String content, Alert.AlertType type) {
        runOnUi(() -> {
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

    public void showBlockingAlert(String title, String content, Alert.AlertType type) {
        runOnUi(() -> {
            VBox alertBox = new VBox(20);
            alertBox.setAlignment(Pos.CENTER); alertBox.setPadding(new Insets(30)); alertBox.setMaxSize(400, 200);
            alertBox.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
            Label tL = new Label(title); tL.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #F2D5A3;");
            Label cL = new Label(content); cL.setStyle("-fx-text-fill: white;"); cL.setWrapText(true); cL.setAlignment(Pos.CENTER);
            Button okBtn = new Button("OK"); okBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white;");
            okBtn.setOnAction(e -> hideModal());
            alertBox.getChildren().addAll(tL, cL, okBtn);
            modalLayer.getChildren().setAll(alertBox);
            modalLayer.setVisible(true);
        });
    }

    public void showInGameMenu() {
        runOnUi(() -> {
            InGameMenuOverlay overlay = new InGameMenuOverlay();
            VBox node = overlay.buildNode(guiController, this::hideModal, modalLayer);
            modalLayer.getChildren().setAll(node);
            modalLayer.setVisible(true);
        });
    }

    public void showSummaryCard() {
        runOnUi(() -> {
            SummaryCardOverlay overlay = new SummaryCardOverlay();
            VBox node = overlay.buildNode(this::hideModal);
            modalLayer.getChildren().setAll(node);
            modalLayer.setVisible(true);
        });
    }

    public void hideModal() {
        runOnUi(() -> {
            modalLayer.setVisible(false);
            modalLayer.getChildren().clear();
        });
    }

    public void showPlayerDisconnectedPopup(String n) {
        runOnUi(() -> {
            VBox alertBox = new VBox(20);
            alertBox.setAlignment(Pos.CENTER); alertBox.setPadding(new Insets(30)); alertBox.setMaxSize(420, 220);
            alertBox.setStyle("-fx-background-color: #1C1C1C; -fx-border-color: #E67E22; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
            Label icon = new Label("📡"); icon.setStyle("-fx-font-size: 32;");
            Label title = new Label("Player Disconnected"); title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #E67E22;");
            Label body = new Label("\"" + n + "\" has disconnected from the game.\nThe game will continue with an auto-player.");
            body.setStyle("-fx-text-fill: white; -fx-font-size: 13;"); body.setWrapText(true); body.setAlignment(Pos.CENTER);
            Button okBtn = new Button("OK"); okBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-font-weight: bold;");
            okBtn.setPrefSize(100, 36); okBtn.setOnAction(e -> hideModal());
            alertBox.getChildren().addAll(icon, title, body, okBtn);
            modalLayer.getChildren().setAll(alertBox);
            modalLayer.setVisible(true);
        });
    }

    public void showConnectionLost() {
        runOnUi(() -> {
            VBox alertBox = new VBox(16);
            alertBox.setAlignment(Pos.CENTER); alertBox.setPadding(new Insets(32)); alertBox.setMaxSize(400, 250);
            alertBox.setStyle("-fx-background-color: #1C1C1C; -fx-border-color: #C0392B; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
            Label icon = new Label("⚠"); icon.setStyle("-fx-font-size: 36; -fx-text-fill: #C0392B;");
            Label title = new Label("Connection to the server has been lost"); title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
            Label subtitle = new Label("Attempting to automatically reconnect..."); subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #AAAAAA;"); subtitle.setWrapText(true);
            ProgressIndicator spinner = new ProgressIndicator(); spinner.setMaxSize(40, 40); spinner.setStyle("-fx-accent: #E67E22;");
            alertBox.getChildren().addAll(icon, title, subtitle, spinner);
            modalLayer.getChildren().setAll(alertBox);
            modalLayer.setVisible(true);
        });
    }

    public void promptConnectionAndRetry(it.polimi.ingsw.am02.client.model.LobbyModel lobbyModel, it.polimi.ingsw.am02.client.view.ClientView clientView) {
        runOnUi(() -> {
            StackPane connectionForm = NetworkPopup.buildNode((config, onError) -> {
                Thread connectionThread = new Thread(() -> {
                    try {
                        ServerProxy newProxy = ServerProxyFactory.create(
                                config.type(), config.host(), config.port(), lobbyModel, clientView);
                        guiController.setServerProxy(newProxy);
                        newProxy.setClientController(guiController);
                        newProxy.connect();
                        guiController.setConnectionConfig(config.type(), config.host(), config.port());
                        showToast("Connected", "Successfully connected via " + config.type(), Alert.AlertType.INFORMATION);

                        hideModal();

                        showProjectInfoScene(this::showIntroScene);

                    } catch (Exception ex) { onError.accept(ex.getMessage()); }
                });
                connectionThread.setDaemon(true);
                connectionThread.start();
            });
            modalLayer.getChildren().setAll(connectionForm);
            modalLayer.setVisible(true);
        });
    }
}