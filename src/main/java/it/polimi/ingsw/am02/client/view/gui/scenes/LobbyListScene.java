package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.List;

public class LobbyListScene {

    private GuiController controller;
    private String customFontFamily = "System";
    private FlowPane lobbyGrid;

    public Scene buildScene(GuiController controller) {
        this.controller = controller;
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        ImageView bgImageView = new ImageView();
        Image bgImage = new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/mesos_lobby.png"));
        bgImageView.setImage(bgImage);
        bgImageView.setPreserveRatio(true);
        bgImageView.fitWidthProperty().bind(root.widthProperty().add(100));

        TranslateTransition move = new TranslateTransition(Duration.seconds(15), bgImageView);
        move.setFromX(-20);
        move.setToX(20);
        move.setFromY(-30);
        move.setToY(30);
        move.setAutoReverse(true);
        move.setCycleCount(TranslateTransition.INDEFINITE);
        move.play();

        ScaleTransition zoom = new ScaleTransition(Duration.seconds(20), bgImageView);
        zoom.setFromX(1.0);
        zoom.setFromY(1.0);
        zoom.setToX(1.05);
        zoom.setToY(1.05);
        zoom.setAutoReverse(true);
        zoom.setCycleCount(ScaleTransition.INDEFINITE);
        zoom.play();

        String fontUrl = getClass().getResource("/it.polimi.ingsw.am02.fonts/tribal.ttf").toExternalForm();
        Font baseFont = Font.loadFont(fontUrl, 10);
        if (baseFont != null) {
            customFontFamily = baseFont.getFamily();
        }

        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(30));

        Label titleLabel = new Label("AVAILABLE LOBBIES");
        titleLabel.setFont(Font.font(customFontFamily, 50));
        titleLabel.setTextFill(Color.web("#F2D5A3"));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(0, 0, 30, 0));
        mainLayout.setTop(titleLabel);

        lobbyGrid = new FlowPane();
        lobbyGrid.setHgap(30);
        lobbyGrid.setVgap(30);
        lobbyGrid.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(lobbyGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
        mainLayout.setCenter(scrollPane);

        StackPane bottomBar = new StackPane();
        bottomBar.setPadding(new Insets(30, 0, 0, 0));

        Button returnBtn = new Button("RECONNECT");
        returnBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 20px; -fx-base: #8B0000; -fx-text-fill: white; -fx-cursor: hand;");
        StackPane.setAlignment(returnBtn, Pos.CENTER_LEFT);

        Button newLobbyBtn = new Button("NEW LOBBY");
        newLobbyBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 24px; -fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
        newLobbyBtn.setPrefSize(250, 60);
        StackPane.setAlignment(newLobbyBtn, Pos.CENTER);
        newLobbyBtn.setOnAction(e -> {
            controller.showLobbyScene();
        });

        Button quitBtn = new Button("QUIT");
        quitBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 20px; -fx-base: #555555; -fx-text-fill: white; -fx-cursor: hand;");
        StackPane.setAlignment(quitBtn, Pos.CENTER_RIGHT);
        quitBtn.setOnAction(e -> {
            if (this.controller != null) this.controller.disconnect();
            System.exit(0);
        });

        bottomBar.getChildren().addAll(returnBtn, newLobbyBtn, quitBtn);
        mainLayout.setBottom(bottomBar);

        root.getChildren().addAll(bgImageView, mainLayout);

        return new Scene(root, 1280, 720);
    }

    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        Platform.runLater(() -> {
            lobbyGrid.getChildren().clear();

            for (LobbyInfo lobby : lobbies) {
                VBox card = createLobbyCard(lobby);
                lobbyGrid.getChildren().add(card);
            }
        });
    }

    private VBox createLobbyCard(LobbyInfo info) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefSize(280, 180);
        card.setStyle("-fx-background-color: rgba(43, 29, 20, 0.85); -fx-border-color: #F2D5A3; -fx-border-width: 3px; -fx-background-radius: 15px; -fx-border-radius: 15px;");

        Label nameLabel = new Label(info.lobbyId());
        nameLabel.setFont(Font.font(customFontFamily, 28));
        nameLabel.setTextFill(Color.web("#F2D5A3"));

        int currentPlayers = info.currentPlayers().size();
        int maxPlayers = info.expectedPlayers();

        Label playersLabel = new Label("PLAYERS: " + currentPlayers + " / " + maxPlayers);
        playersLabel.setFont(Font.font(customFontFamily, 20));
        playersLabel.setTextFill(Color.web("#A9A9A9"));

        Button joinBtn = new Button("JOIN LOBBY");
        joinBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 18px; -fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");

        if (currentPlayers >= maxPlayers) {
            joinBtn.setDisable(true);
            joinBtn.setText("LOBBY FULL");
            joinBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 18px; -fx-base: #8B0000; -fx-text-fill: white;");
        }

        joinBtn.setOnAction(e -> {
        });

        card.getChildren().addAll(nameLabel, playersLabel, joinBtn);
        return card;
    }
}