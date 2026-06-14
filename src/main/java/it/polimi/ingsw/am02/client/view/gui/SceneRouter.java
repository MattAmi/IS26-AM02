package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.gui.scenes.*;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * Router class for managing GUI scenes and navigation.
 * This class handles switching between different views and displaying overlays,
 * alerts, and notifications.
 */
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

    /**
     * Constructs a new SceneRouter.
     *
     * @param primaryStage The primary stage of the JavaFX application.
     */
    public SceneRouter(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initWindowArchitecture();
    }

    /**
     * Sets the GUI controller for the router.
     *
     * @param guiController The GuiController instance.
     */
    public void setGuiController(GuiController guiController) {
        this.guiController = guiController;
    }

    /**
     * Initializes the basic window architecture with layers for base content,
     * modals, and toast notifications.
     */
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

    /**
     * Utility method to run a task on the JavaFX Application Thread.
     *
     * @param action The Runnable task to execute.
     */
    private void runOnUi(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Shows the primary stage.
     */
    public void show() {
        runOnUi(() -> {
            primaryStage.show();
            // Bring the window (and thus the connection popup) to the foreground so
            // the user doesn't have to hunt for it behind other windows at launch.
            primaryStage.setIconified(false);
            primaryStage.setAlwaysOnTop(true);
            primaryStage.toFront();
            primaryStage.requestFocus();
            primaryStage.setAlwaysOnTop(false);
        });
    }

    /**
     * Switches the base view to a new node.
     *
     * @param newView The new Region to display.
     */
    private void switchView(Region newView) {
        runOnUi(() -> baseLayer.getChildren().setAll(newView));
    }

    /**
     * Displays the project information scene.
     *
     * @param onExit Callback to execute when exiting the scene.
     */
    public void showProjectInfoScene(Runnable onExit) {
        runOnUi(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = null;
            ProjectInfoScene infoScene = new ProjectInfoScene();
            switchView(infoScene.buildNode(onExit));
        });
    }

    /**
     * Displays the introduction scene.
     */
    public void showIntroScene() {
        runOnUi(() -> {
            primaryStage.setResizable(true);
            primaryStage.setFullScreen(true);
            primaryStage.centerOnScreen();
            switchView(new IntroScene().buildNode(this::showGameMenuScene));
        });
    }

    /**
     * Displays the game menu scene.
     */
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

    /**
     * Displays the lobby list scene.
     *
     * @param lobbies The list of initial lobbies to display.
     */
    public void showLobbyListScene(List<LobbyInfo> lobbies) {
        runOnUi(() -> {
            this.gameScene = null;
            this.lobbyScene = null;
            this.lobbyListScene = new LobbyListScene();
            switchView(lobbyListScene.buildNode(guiController, this::showGameMenuScene));
            if (lobbies != null) lobbyListScene.onAvailableLobbiesUpdated(lobbies);
        });
    }

    /**
     * Displays the current lobby scene.
     *
     * @param currentLobby The information of the lobby to display.
     */
    public void showLobbyScene(LobbyInfo currentLobby) {
        runOnUi(() -> {
            this.gameScene = null;
            this.lobbyListScene = null;
            this.lobbyScene = new LobbyScene();
            switchView(lobbyScene.buildNode(guiController));
            if (currentLobby != null) lobbyScene.updateLobbyState(currentLobby);
        });
    }

    /**
     * Switches to the game scene.
     *
     * @param gameModel The game model to use for the scene.
     */
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

    /**
     * Returns the current LobbyListScene instance.
     *
     * @return The LobbyListScene or null if not active.
     */
    public LobbyListScene getLobbyListScene() { return lobbyListScene; }

    /**
     * Returns the current LobbyScene instance.
     *
     * @return The LobbyScene or null if not active.
     */
    public LobbyScene getLobbyScene() { return lobbyScene; }

    /**
     * Returns the current GameScene instance.
     *
     * @return The GameScene or null if not active.
     */
    public GameScene getGameScene() { return gameScene; }

    /**
     * Shows a toast notification.
     *
     * @param title   The title of the toast.
     * @param content The message content.
     * @param type    The type of alert for styling purposes.
     */
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

    /**
     * Displays a blocking alert modal.
     *
     * @param title   The title of the alert.
     * @param content The message content.
     * @param type    The alert type.
     */
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

    /**
     * Displays a game over popup with options to return to menu or exit.
     *
     * @param title   The title of the popup.
     * @param content The message content.
     */
    public void showGameOverPopup(String title, String content) {
        runOnUi(() -> {
            VBox alertBox = new VBox(20);
            alertBox.setAlignment(Pos.CENTER); alertBox.setPadding(new Insets(30)); alertBox.setMaxSize(450, 250);
            alertBox.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

            Label tL = new Label(title); tL.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F2D5A3;");
            Label cL = new Label(content); cL.setStyle("-fx-text-fill: white; -fx-font-size: 14px;"); cL.setWrapText(true); cL.setAlignment(Pos.CENTER);

            alertBox.getChildren().addAll(tL, cL, buildGameOverButtons());

            modalLayer.getChildren().setAll(alertBox);
            modalLayer.setVisible(true);
        });
    }

    /**
     * Displays the end-of-game popup with the winners and the full final
     * rankings, mirroring the TUI's game-over screen. Each ranking row shows the
     * position, the player's nickname and their total prestige points next to the
     * prestige-point icon.
     *
     * @param title         The title of the popup.
     * @param winners       The nicknames of the winning players.
     * @param finalRankings The final score rankings, in ranking order.
     */
    public void showGameOverPopup(String title, List<String> winners, List<PlayerFinalScore> finalRankings) {
        runOnUi(() -> {
            VBox alertBox = new VBox(16);
            alertBox.setAlignment(Pos.CENTER); alertBox.setPadding(new Insets(30)); alertBox.setMaxSize(440, 500);

            Label tL = new Label(title); tL.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F2D5A3;");
            Label wL = new Label((winners.size() == 1 ? "Winner: " : "Winners: ") + String.join(", ", winners));
            wL.setStyle("-fx-text-fill: white; -fx-font-size: 14px;"); wL.setWrapText(true); wL.setAlignment(Pos.CENTER);

            Label rankHeader = new Label("FINAL RANKINGS");
            rankHeader.setStyle("-fx-text-fill: #C9A0DC; -fx-font-size: 13px; -fx-font-weight: bold;");

            VBox rankingsBox = new VBox(8);
            rankingsBox.setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < finalRankings.size(); i++) {
                PlayerFinalScore s = finalRankings.get(i);

                Label pos = new Label((i + 1) + ".");
                pos.setStyle("-fx-text-fill: #F2D5A3; -fx-font-weight: bold; -fx-font-size: 13px;");
                pos.setMinWidth(22);

                Label name = new Label(s.nickname());
                name.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);
                name.setMaxWidth(Double.MAX_VALUE);

                ImageView ppIcon = new ImageView(ImageLoader.getImage("/images/icons/prestige_point.png"));
                ppIcon.setFitHeight(16); ppIcon.setPreserveRatio(true);

                Label ppLabel = new Label(String.valueOf(s.totalPrestigePoints()));
                ppLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

                HBox row = new HBox(8, pos, name, ppIcon, ppLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                rankingsBox.getChildren().add(row);
            }

            ScrollPane scroll = new ScrollPane(rankingsBox);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(220);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

            alertBox.getChildren().addAll(tL, wL, rankHeader, scroll, buildGameOverButtons());

            // Background: the box art (mesos_box.png) zoomed onto the central
            // bonfire-and-dancers scene via a viewport crop, dimmed by a dark
            // scrim so the white/gold text stays legible.
            ImageView bg = new ImageView(ImageLoader.getImage("/images/mesos_box.png"));
            bg.setPreserveRatio(false);
            // Crop region (in source-image pixels) around the fire and figures.
            bg.setViewport(new Rectangle2D(95, 175, 410, 400));

            Region scrim = new Region();
            scrim.setStyle("-fx-background-color: rgba(20, 10, 6, 0.72);");

            StackPane backdrop = new StackPane(bg, scrim);
            bg.fitWidthProperty().bind(backdrop.widthProperty());
            bg.fitHeightProperty().bind(backdrop.heightProperty());

            Rectangle clip = new Rectangle();
            clip.setArcWidth(22); clip.setArcHeight(22);
            clip.widthProperty().bind(backdrop.widthProperty());
            clip.heightProperty().bind(backdrop.heightProperty());
            backdrop.setClip(clip);

            StackPane card = new StackPane(backdrop, alertBox);
            card.setMaxSize(440, 500);
            card.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; "
                    + "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

            modalLayer.getChildren().setAll(card);
            modalLayer.setVisible(true);
        });
    }

    /**
     * Builds the shared "return to menu / exit" button row used by the
     * game-over popups.
     *
     * @return the configured button bar.
     */
    private HBox buildGameOverButtons() {
        HBox buttons = new HBox(20);
        buttons.setAlignment(Pos.CENTER);

        Button menuBtn = new Button("RETURN TO MENU");
        menuBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-font-weight: bold;");
        menuBtn.setPrefHeight(40);
        menuBtn.setOnAction(e -> {
            hideModal();
            guiController.requestReturnToLobby();
        });

        Button exitBtn = new Button("EXIT GAME");
        exitBtn.setStyle("-fx-base: #8B0000; -fx-text-fill: white; -fx-font-weight: bold;");
        exitBtn.setPrefHeight(40);
        exitBtn.setOnAction(e -> System.exit(0));

        buttons.getChildren().addAll(menuBtn, exitBtn);
        return buttons;
    }

    /**
     * Displays the in-game menu overlay.
     */
    public void showInGameMenu() {
        runOnUi(() -> {
            InGameMenuOverlay overlay = new InGameMenuOverlay();
            VBox node = overlay.buildNode(guiController, this::hideModal, modalLayer);
            modalLayer.getChildren().setAll(node);
            modalLayer.setVisible(true);
        });
    }

    /**
     * Displays the summary card overlay.
     */
    public void showSummaryCard() {
        runOnUi(() -> {
            SummaryCardOverlay overlay = new SummaryCardOverlay();
            VBox node = overlay.buildNode(this::hideModal);
            modalLayer.getChildren().setAll(node);
            modalLayer.setVisible(true);
        });
    }

    /**
     * Hides any visible modal overlay.
     */
    public void hideModal() {
        runOnUi(() -> {
            modalLayer.setVisible(false);
            modalLayer.getChildren().clear();
        });
    }

    /**
     * Displays a popup when a player disconnects.
     *
     * @param n The nickname of the disconnected player.
     */
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

    /**
     * Displays an overlay indicating that connection to the server was lost.
     */
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

    /**
     * Prompts the user for connection details and attempts to connect.
     *
     * @param lobbyModel The lobby model to update upon connection.
     * @param clientView The client view to associate with the connection.
     */
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

                        // Once IP/port are set and we're connected, switch the window
                        // to full screen right away so the whole experience (starting
                        // with the project-info scene) is shown maximised.
                        runOnUi(() -> {
                            primaryStage.setResizable(true);
                            primaryStage.setFullScreen(true);
                            primaryStage.centerOnScreen();
                        });

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