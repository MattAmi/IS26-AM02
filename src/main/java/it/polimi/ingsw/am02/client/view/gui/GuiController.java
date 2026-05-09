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
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

public class GuiController extends ClientController {

    private final Stage primaryStage;
    private final ClientView clientView;

    // --- ARCHITETTURA SPA ---
    private StackPane rootContainer;
    private StackPane baseLayer;    // Contiene il gioco/lobby
    private StackPane modalLayer;   // Overlay per i popup (es. Errore o Rete)
    private VBox toastLayer;        // Contenitore per notifiche non bloccanti

    // Riferimenti alle scene correnti
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

    private void initWindowArchitecture() {
        baseLayer = new StackPane();
        baseLayer.setStyle("-fx-background-color: #1a1a1a;");

        modalLayer = new StackPane();
        modalLayer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        modalLayer.setVisible(false);
        modalLayer.setAlignment(Pos.CENTER);

        toastLayer = new VBox(10);
        toastLayer.setAlignment(Pos.TOP_RIGHT);
        toastLayer.setPadding(new Insets(20));
        toastLayer.setPickOnBounds(false);

        rootContainer = new StackPane(baseLayer, modalLayer, toastLayer);
        Scene mainScene = new Scene(rootContainer, 1280, 800);

        primaryStage.setTitle("Mesos - Board Game");
        primaryStage.setScene(mainScene);
        primaryStage.setResizable(true);
    }

    public void start() {
        primaryStage.show();
        // Appena si apre la finestra, chiediamo i dati di rete!
        promptConnectionAndRetry();
    }

    // --- NAVIGAZIONE SPA ---

    private void switchView(Region newView) {
        Platform.runLater(() -> {
            baseLayer.getChildren().setAll(newView);
        });
    }

    public void showLoginScene() {
        // NOTA: le tue scene ora devono restituire un Region (es. BorderPane), non più una Scene!
        Platform.runLater(() -> switchView(new LoginScene().buildNode(this::showIntroScene)));
    }

    public void showIntroScene() {
        Platform.runLater(() -> switchView(new IntroScene().buildNode(this::showLobbyListScene)));
    }

    public void showLobbyListScene() {
        Platform.runLater(() -> {
            this.lobbyScene = null;
            this.lobbyListScene = new LobbyListScene();
            switchView(lobbyListScene.buildNode(this));
            handleAvailableLobbiesUpdated(lobbyModel.getAvailableLobbies());
        });
    }

    public void showLobbyScene() {
        Platform.runLater(() -> {
            this.lobbyScene = new LobbyScene();
            switchView(lobbyScene.buildNode(this));
            if (lobbyModel.getCurrentLobby() != null) {
                lobbyScene.updateLobbyState(lobbyModel.getCurrentLobby());
            }
        });
    }

    public void switchToGameScene(String gameId) {
        Platform.runLater(() -> {
            this.gameScene = new GameScene();
            switchView(gameScene.buildNode(this));
        });
    }

    // --- AZIONI CHIAMATE DALLE SCENE ---
    public void requestSetUsername(String nickname) { proxy.requestSetUsername(nickname); }
    public void requestCreateLobby(int size) { proxy.requestCreateLobby(size); }
    public void requestJoinLobby(String lobbyId) {
        List<LobbyInfo> lobbies = lobbyModel.getAvailableLobbies();
        for(int i=0; i<lobbies.size(); i++) {
            if(lobbies.get(i).lobbyId().equals(lobbyId)) { proxy.requestJoinLobby(lobbyId); return; }
        }
    }
    public void requestReconnect(String nick, String gId) { proxy.requestReconnect(nick, gId); }
    public void requestSelectTotem(Totem t) { proxy.requestSelectTotem(t); }
    public void requestLeaveLobby() {
        if (lobbyModel.getCurrentLobby() != null) {
            proxy.requestLeaveLobby();
            // handleReturnToLobby() verrà chiamato quando il server
            // risponde con onAvailableLobbiesUpdated
        } else {
            handleReturnToLobby();
        }
    }
    public void moveTotem(char t) { proxy.moveTotem(t); }
    public void resolveActions(List<String> ids) { proxy.resolveActions(ids); }

    // --- NOTIFICHE DAL MODELLO ---
    public void refreshFullGameScene(GameModel gameModel) { if (gameScene != null) gameScene.refreshAll(gameModel); }

    public void handleUsernameResult(String username, boolean accepted, String reason) {
        Platform.runLater(() -> {
            if (!accepted) { showToast("Nickname Rifiutato", reason, Alert.AlertType.WARNING); if (lobbyScene != null) lobbyScene.onNicknameRejected(); }
            else { if (lobbyScene != null) lobbyScene.onNicknameAccepted(username); else showIntroScene(); }
        });
    }

    public void handleAvailableLobbiesUpdated(List<LobbyInfo> lobbies) { Platform.runLater(() -> { if (lobbyListScene != null) lobbyListScene.onAvailableLobbiesUpdated(lobbies); }); }
    public void handleCurrentLobbyUpdated(LobbyInfo lobby) { Platform.runLater(() -> { if (lobbyScene != null) lobbyScene.updateLobbyState(lobby); }); }
    public void handleLobbyDissolved() { showBlockingAlert("Lobby Chiusa", "La lobby è stata chiusa.", Alert.AlertType.INFORMATION); showLobbyListScene(); }

    public void handleGameSetupCompleted(List<String> t, Map<String, Integer> f, BoardSnapshot b) { refreshFullGameScene(getGameModel()); }
    public void handlePhaseChanged(PhaseType p, String c, List<String> r) { refreshFullGameScene(getGameModel()); }
    public void handleCurrentPlayerChanged(String n) { refreshFullGameScene(getGameModel()); }
    public void handleTurnOrderEstablished(List<String> t) { refreshFullGameScene(getGameModel()); }
    public void handleTotemPlaced(String n, char t) { refreshFullGameScene(getGameModel()); }
    public void handleTotemReturned(String n, int p) { refreshFullGameScene(getGameModel()); }
    public void handleOfferTilesUpdated(List<OfferTileInfo> o) { refreshFullGameScene(getGameModel()); }
    public void handleBoardUpdated(List<String> u, List<String> l, int d) { refreshFullGameScene(getGameModel()); }
    public void handleEraChanged(List<String> u, List<String> l) { refreshFullGameScene(getGameModel()); }
    public void handlePlayerLimitsInitialized(String n, int u, int l) { refreshFullGameScene(getGameModel()); }
    public void handlePlayerLimitsUpdated(String n, int u, int l) { refreshFullGameScene(getGameModel()); }
    public void handlePlayerResourceChanged(String n, ResourceType r, int v) { refreshFullGameScene(getGameModel()); }
    public void handleCardTaken(String n, String c, CardType t, RowPosition s) { refreshFullGameScene(getGameModel()); }
    public void handleExtraTurnStarted(String n, int u, int l) { refreshFullGameScene(getGameModel()); }
    public void handleEventResolved(String id, String n) { refreshFullGameScene(getGameModel()); }
    public void handleShowAvailableTotems() { }
    public void handleExtraTurnEnded(String n) { refreshFullGameScene(getGameModel()); }

    public void handleGameEnded(List<String> w, List<PlayerFinalScore> r) { showBlockingAlert("Game Over", "Vincitori: " + w, Alert.AlertType.INFORMATION); }
    public void handlePlayerDisconnected(String n) { if (gameScene != null) gameScene.setPlayerOffline(n); }
    public void handleGameAborted(String l) { showBlockingAlert("Partita Annullata", "Vincitore per abbandono: " + l, Alert.AlertType.INFORMATION); handleReturnToLobby(); }
    public void handleGameRecoveryFailed() { showBlockingAlert("Errore", "Recupero partita fallito", Alert.AlertType.ERROR); handleReturnToLobby(); }
    public void handlePlayerReconnected(String n) { if (gameScene != null) gameScene.setPlayerOnline(n); }

    public void handleError(String message) { showToast("Errore", message, Alert.AlertType.WARNING); }
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
    public void handleReturnToLobby() {
        Platform.runLater(() -> {
            performReturnToLobby();
            this.gameScene = null;
            this.lobbyScene = null;
            showLobbyListScene();
        });
    }

    // --- UI SPA (TOAST E MODAL NATIVI) ---

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

    public void promptConnectionAndRetry() {
        Platform.runLater(() -> {
            // Generiamo il form passando i dati config e la funzione onError
            VBox connectionForm = NetworkPopup.buildNode((config, onError) -> {

                Thread connectionThread = new Thread(() -> {
                    try {
                        ServerProxy newProxy = ServerProxyFactory.create(
                                config.type(), config.host(), config.port(), lobbyModel, clientView);

                        this.setServerProxy(newProxy);
                        newProxy.setClientController(this);
                        newProxy.connect();

                        // Connessione OK!
                        Platform.runLater(() -> {
                            modalLayer.setVisible(false);
                            showToast("Connected", "Connessione stabilita via " + config.type(), Alert.AlertType.INFORMATION);
                            showIntroScene(); // Andiamo dritti all'Intro!
                        });
                    } catch (Exception ex) {
                        // Connessione FALLITA!
                        // Invece di distruggere il form con showBlockingAlert, diciamo al form di mostrare l'errore.
                        onError.accept(ex.getMessage());
                    }
                });
                connectionThread.setDaemon(true);
                connectionThread.start();
            });

            // Mostriamo il form
            modalLayer.getChildren().setAll(connectionForm);
            modalLayer.setVisible(true);
        });
    }
}