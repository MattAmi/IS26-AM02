package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The Main Menu of the game.
 * Acts as the central hub to navigate to Lobby creation, joining, or reconnecting.
 */
public class GameMenuScene {

    private GuiController controller;
    private StackPane menuContainer;
    private VBox mainButtonsBox;
    private VBox createLobbyBox;
    private VBox reconnectBox;

    public Region buildNode(GuiController controller) {
        this.controller = controller;

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #1a1a1a;");

        // Title
        Label title = new Label("MESOS");
        title.setTextFill(Color.web("#F2D5A3"));
        title.setFont(Font.font("System", FontWeight.BOLD, 48));
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        StackPane.setMargin(title, new Insets(50, 0, 0, 0));

        menuContainer = new StackPane();
        menuContainer.setMaxSize(400, 500);

        buildMainButtons();
        buildCreateLobbyForm();
        buildReconnectForm();

        // Initially show only the main buttons
        menuContainer.getChildren().addAll(createLobbyBox, reconnectBox, mainButtonsBox);
        createLobbyBox.setVisible(false);
        reconnectBox.setVisible(false);

        root.getChildren().addAll(title, menuContainer);
        return root;
    }

    private void buildMainButtons() {
        mainButtonsBox = new VBox(20);
        mainButtonsBox.setAlignment(Pos.CENTER);

        Button createBtn = createMenuButton("CREATE LOBBY");
        createBtn.setOnAction(e -> switchInternalMenu(createLobbyBox));

        Button joinBtn = createMenuButton("JOIN LOBBY");
        joinBtn.setOnAction(e -> controller.showLobbyListScene());

        Button reconnectBtn = createMenuButton("RECONNECT TO GAME");
        reconnectBtn.setOnAction(e -> switchInternalMenu(reconnectBox));

        Button rulesBtn = createMenuButton("GAME RULES");
        rulesBtn.setOnAction(e -> controller.handleError("Game Rules PDF integration coming soon!"));

        Button quitBtn = createMenuButton("QUIT GAME");
        quitBtn.setStyle("-fx-base: #8B0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        quitBtn.setOnAction(e -> System.exit(0));

        mainButtonsBox.getChildren().addAll(createBtn, joinBtn, reconnectBtn, rulesBtn, quitBtn);
    }

    private void buildCreateLobbyForm() {
        createLobbyBox = new VBox(15);
        createLobbyBox.setAlignment(Pos.CENTER);
        createLobbyBox.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 30;");

        Label lbl = new Label("Select Lobby Size (2-4):");
        lbl.setTextFill(Color.WHITE);
        lbl.setFont(Font.font("System", 16));

        TextField sizeField = new TextField("2");
        sizeField.setMaxWidth(100);
        sizeField.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("CONFIRM");
        confirmBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white;");
        confirmBtn.setOnAction(e -> {
            try {
                int size = Integer.parseInt(sizeField.getText());
                if (size >= 2 && size <= 4) {
                    controller.requestCreateLobby(size);
                } else {
                    controller.handleError("Size must be between 2 and 4.");
                }
            } catch (NumberFormatException ex) {
                controller.handleError("Invalid number format.");
            }
        });

        Button backBtn = new Button("BACK");
        backBtn.setOnAction(e -> switchInternalMenu(mainButtonsBox));

        createLobbyBox.getChildren().addAll(lbl, sizeField, confirmBtn, backBtn);
    }

    private void buildReconnectForm() {
        reconnectBox = new VBox(15);
        reconnectBox.setAlignment(Pos.CENTER);
        reconnectBox.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 30;");

        Label lbl1 = new Label("Nickname:");
        lbl1.setTextFill(Color.WHITE);
        TextField nickField = new TextField();
        nickField.setMaxWidth(200);

        Label lbl2 = new Label("Game ID:");
        lbl2.setTextFill(Color.WHITE);
        TextField gameIdField = new TextField();
        gameIdField.setMaxWidth(200);

        Button confirmBtn = new Button("RECONNECT");
        confirmBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white;");
        confirmBtn.setOnAction(e -> {
            if (!nickField.getText().isBlank() && !gameIdField.getText().isBlank()) {
                controller.requestReconnect(nickField.getText(), gameIdField.getText());
            } else {
                controller.handleError("Please fill both fields.");
            }
        });

        Button backBtn = new Button("BACK");
        backBtn.setOnAction(e -> switchInternalMenu(mainButtonsBox));

        reconnectBox.getChildren().addAll(lbl1, nickField, lbl2, gameIdField, confirmBtn, backBtn);
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(250, 50);
        btn.setStyle("-fx-base: #3e2a1d; -fx-text-fill: #F2D5A3; -fx-font-weight: bold; -fx-font-size: 16px; -fx-cursor: hand;");
        return btn;
    }

    private void switchInternalMenu(VBox menuToShow) {
        mainButtonsBox.setVisible(false);
        createLobbyBox.setVisible(false);
        reconnectBox.setVisible(false);
        menuToShow.setVisible(true);
    }
}