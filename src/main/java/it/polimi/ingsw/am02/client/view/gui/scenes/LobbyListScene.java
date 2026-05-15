package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;

public class LobbyListScene {

    private GuiController controller;
    private VBox listContainer;
    private Font tribalSmall;
    private Font tribalMedium;
    private Font tribalLarge;
    private Font introFont;

    public Region buildNode(GuiController controller, Runnable onBack) {
        this.controller = controller;

        try {
            tribalSmall = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/tribal.ttf"), 14);
            tribalMedium = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/tribal.ttf"), 20);
            tribalLarge = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/tribal.ttf"), 36);
            introFont = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 18);
        } catch (Exception ignored) {}

        StackPane rootNode = new StackPane();
        rootNode.setStyle("-fx-background-color: #000000;");

        Pane bgContainer = new Pane();
        bgContainer.setMouseTransparent(true);
        bgContainer.prefWidthProperty().bind(rootNode.widthProperty());
        bgContainer.prefHeightProperty().bind(rootNode.heightProperty());

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(rootNode.widthProperty());
        clip.heightProperty().bind(rootNode.heightProperty());
        bgContainer.setClip(clip);

        ImageView backgroundView = new ImageView();
        try {
            Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/mesos_lobby.png")));
            backgroundView.setImage(img);
            double iw = img.getWidth();
            double ih = img.getHeight();
            double vw = iw * 0.6;
            double vh = ih * 0.6;
            double vx = (iw - vw) / 2;
            double vy = (ih - vh) / 2;
            backgroundView.setViewport(new Rectangle2D(vx, vy, vw, vh));
        } catch (Exception ignored) {}

        backgroundView.fitWidthProperty().bind(bgContainer.widthProperty());
        backgroundView.fitHeightProperty().bind(bgContainer.heightProperty());
        backgroundView.setPreserveRatio(false);

        backgroundView.setEffect(new GaussianBlur(12));

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2.0), backgroundView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        ScaleTransition st = new ScaleTransition(Duration.seconds(20), backgroundView);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.25); st.setToY(1.25);
        st.setCycleCount(ScaleTransition.INDEFINITE); st.setAutoReverse(true); st.play();

        bgContainer.getChildren().add(backgroundView);
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.45);");
        overlay.setMouseTransparent(true);
        overlay.prefWidthProperty().bind(rootNode.widthProperty());
        overlay.prefHeightProperty().bind(rootNode.heightProperty());

        BorderPane uiLayer = new BorderPane();
        uiLayer.setPadding(new Insets(30));
        uiLayer.setPickOnBounds(false);

        StackPane topBar = new StackPane();
        topBar.setPadding(new Insets(0, 0, 20, 0));

        Button backBtn = new Button("BACK TO MENU");
        backBtn.setStyle("-fx-base: #444; -fx-text-fill: white; -fx-cursor: hand;");
        if (tribalSmall != null) backBtn.setFont(tribalSmall);
        backBtn.setOnAction(e -> onBack.run());
        StackPane.setAlignment(backBtn, Pos.CENTER_LEFT);

        Label title = new Label("AVAILABLE LOBBIES");
        title.setTextFill(Color.web("#F2D5A3"));
        if (tribalLarge != null) title.setFont(tribalLarge);
        StackPane.setAlignment(title, Pos.CENTER);

        topBar.getChildren().addAll(title, backBtn);
        uiLayer.setTop(topBar);

        listContainer = new VBox(20);
        listContainer.setAlignment(Pos.TOP_CENTER);
        listContainer.setPadding(new Insets(100, 20, 20, 20));

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-viewport-background: transparent;");

        uiLayer.setCenter(scroll);
        rootNode.getChildren().addAll(bgContainer, overlay, uiLayer);

        return rootNode;
    }

    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        Platform.runLater(() -> {
            listContainer.getChildren().clear();

            if (lobbies == null || lobbies.isEmpty()) {
                Label emptyLbl = new Label("No active lobbies found. Go back and create one!");
                emptyLbl.setTextFill(Color.WHITE);
                if (tribalMedium != null) emptyLbl.setFont(tribalMedium);
                listContainer.getChildren().add(emptyLbl);
                return;
            }

            for (LobbyInfo lobby : lobbies) {
                HBox row = new HBox(30);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(20));
                row.setStyle("-fx-background-color: rgba(43, 29, 20, 0.85); -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 10;");

                TextField idField = new TextField("Lobby ID: " + lobby.lobbyId());
                idField.setEditable(false); idField.setFocusTraversable(false);
                idField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 0; -fx-cursor: text;");
                if (introFont != null) idField.setFont(introFont);
                idField.setMinWidth(Region.USE_PREF_SIZE);
                HBox.setHgrow(idField, Priority.NEVER);

                Label players = new Label("Players: " + lobby.currentPlayers().size() + " / " + lobby.expectedPlayers());
                players.setTextFill(Color.LIGHTGRAY);
                if (introFont != null) players.setFont(introFont);
                players.setMinWidth(Region.USE_PREF_SIZE);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button join = new Button("JOIN LOBBY");
                join.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
                if (tribalSmall != null) join.setFont(tribalSmall);
                join.setDisable(lobby.currentPlayers().size() >= lobby.expectedPlayers());
                join.setOnAction(e -> controller.requestJoinLobby(lobby.lobbyId()));

                row.getChildren().addAll(idField, players, spacer, join);
                listContainer.getChildren().add(row);
            }
        });
    }
}