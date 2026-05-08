package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LobbyListScene {

    private GuiController controller;
    private FlowPane lobbyGrid;

    public Scene buildScene(GuiController controller) {
        this.controller = controller;
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b1d14;");

        Label title = new Label("MESOS - LOBBY SELECTION");
        title.setTextFill(Color.web("#F2D5A3"));
        title.setFont(Font.font("System", FontWeight.BOLD, 36));
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20));
        root.setTop(title);

        lobbyGrid = new FlowPane(20, 20);
        lobbyGrid.setPadding(new Insets(20));
        lobbyGrid.setAlignment(Pos.CENTER);
        
        ScrollPane scroll = new ScrollPane(lobbyGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scroll);

        HBox bottomBar = new HBox(20);
        bottomBar.setPadding(new Insets(20));
        bottomBar.setAlignment(Pos.CENTER);
        
        Button newLobbyBtn = new Button("CREATE NEW LOBBY");
        newLobbyBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-font-size: 18;");
        newLobbyBtn.setOnAction(e -> handleCreateLobby());

        Button reconnectBtn = new Button("RECONNECT");
        reconnectBtn.setStyle("-fx-base: #2B547E; -fx-text-fill: white; -fx-font-size: 18;");
        reconnectBtn.setOnAction(e -> handleReconnect());

        Button quitBtn = new Button("QUIT");
        quitBtn.setStyle("-fx-base: #8B0000; -fx-text-fill: white; -fx-font-size: 18;");
        quitBtn.setOnAction(e -> System.exit(0));

        bottomBar.getChildren().addAll(newLobbyBtn, reconnectBtn, quitBtn);
        root.setBottom(bottomBar);

        return new Scene(root, 1024, 768);
    }

    private void handleReconnect() {
        TextInputDialog nickDialog = new TextInputDialog();
        nickDialog.setTitle("Reconnect");
        nickDialog.setHeaderText("Enter your nickname used in the game:");
        nickDialog.showAndWait().ifPresent(nick -> {
            TextInputDialog idDialog = new TextInputDialog();
            idDialog.setTitle("Reconnect");
            idDialog.setHeaderText("Enter the Game ID:");
            idDialog.showAndWait().ifPresent(gameId -> {
                controller.requestReconnect(nick, gameId);
                controller.switchToGameScene(gameId);
            });
        });
    }

    private void handleCreateLobby() {
        List<Integer> choices = Arrays.asList(2, 3, 4, 5);
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(2, choices);
        dialog.setTitle("New Lobby");
        dialog.setHeaderText("Create a new game lobby");
        dialog.setContentText("Choose number of players:");

        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(size -> {
            controller.requestCreateLobby(size);
            controller.showLobbyScene();
        });
    }

    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        Platform.runLater(() -> {
            lobbyGrid.getChildren().clear();
            for (LobbyInfo info : lobbies) {
                lobbyGrid.getChildren().add(createLobbyCard(info));
            }
        });
    }

    private VBox createLobbyCard(LobbyInfo info) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #3e2a1d; -fx-border-color: #F2D5A3; -fx-border-radius: 10; -fx-background-radius: 10;");
        card.setPrefSize(200, 150);
        card.setAlignment(Pos.CENTER);

        Label idLabel = new Label("Lobby ID: " + info.lobbyId().substring(0, 8));
        idLabel.setTextFill(Color.WHITE);
        
        Label playersLabel = new Label("Players: " + info.currentPlayers().size() + " / " + info.expectedPlayers());
        playersLabel.setTextFill(Color.LIGHTGRAY);

        Button joinBtn = new Button("JOIN");
        joinBtn.setDisable(info.currentPlayers().size() >= info.expectedPlayers());
        joinBtn.setOnAction(e -> {
            controller.requestJoinLobby(info.lobbyId());
            controller.showLobbyScene();
        });

        card.getChildren().addAll(idLabel, playersLabel, joinBtn);
        return card;
    }
}
