package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Scene dedicated exclusively to browsing and joining existing lobbies.
 */
public class LobbyListScene {

    private GuiController controller;
    private VBox listContainer;

    public Region buildNode(GuiController controller) {
        this.controller = controller;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");
        root.setPadding(new Insets(30));

        // Top: Header & Back Button
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← BACK TO MENU");
        backBtn.setStyle("-fx-base: #444; -fx-text-fill: white; -fx-cursor: hand;");
        backBtn.setOnAction(e -> controller.showGameMenuScene());

        Label header = new Label("AVAILABLE LOBBIES");
        header.setTextFill(Color.web("#F2D5A3"));
        header.setFont(Font.font("System", FontWeight.BOLD, 28));

        topBar.getChildren().addAll(backBtn, header);
        root.setTop(topBar);

        // Center: Lobby List
        listContainer = new VBox(15);
        listContainer.setAlignment(Pos.TOP_CENTER);
        listContainer.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scrollPane);

        return root;
    }

    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        listContainer.getChildren().clear();

        if (lobbies == null || lobbies.isEmpty()) {
            Label emptyLbl = new Label("No active lobbies found. Go back and create one!");
            emptyLbl.setTextFill(Color.GRAY);
            emptyLbl.setFont(Font.font("System", 16));
            listContainer.getChildren().add(emptyLbl);
            return;
        }

        for (LobbyInfo lobby : lobbies) {
            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(15));
            row.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 1; -fx-border-radius: 5;");

            Label idLbl = new Label("Lobby ID: " + lobby.lobbyId());
            idLbl.setTextFill(Color.WHITE);
            idLbl.setPrefWidth(200);

            Label playersLbl = new Label("Players: " + lobby.currentPlayers().size() + " / " + lobby.expectedPlayers());
            playersLbl.setTextFill(Color.LIGHTGRAY);
            playersLbl.setPrefWidth(150);

            Button joinBtn = new Button("JOIN");
            joinBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
            joinBtn.setDisable(lobby.currentPlayers().size() >= lobby.expectedPlayers());
            joinBtn.setOnAction(e -> controller.requestJoinLobby(lobby.lobbyId()));

            row.getChildren().addAll(idLbl, playersLbl, joinBtn);
            listContainer.getChildren().add(row);
        }
    }
}