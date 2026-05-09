package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

public class LobbyScene {

    private GuiController controller;
    private VBox playerList;
    private VBox nicknameBox;
    private VBox totemBox;
    private TextField nickField;
    private final Map<Totem, Button> totemButtons = new HashMap<>();
    private String myNickname;

    // FIX 1: Rinominato da buildScene a buildNode
    public Region buildNode(GuiController controller) {
        this.controller = controller;
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label header = new Label("LOBBY PREPARATION");
        header.setTextFill(Color.web("#F2D5A3"));
        header.setFont(Font.font("System", FontWeight.BOLD, 24));
        header.setPadding(new Insets(20));
        root.setTop(header);

        StackPane centerStack = new StackPane();

        // Phase 1: Nickname
        nicknameBox = new VBox(15);
        nicknameBox.setAlignment(Pos.CENTER);
        Label nickLabel = new Label("Choose your nickname:");
        nickLabel.setTextFill(Color.WHITE);
        nickField = new TextField();
        nickField.setMaxWidth(300);
        Button confirmNick = new Button("CONFIRM NICKNAME");
        confirmNick.setOnAction(e -> {
            if(!nickField.getText().isBlank()) {
                nickField.setEditable(false);
                // FIX 2: Usiamo requestSetUsername invece di handleSetNickname
                controller.requestSetUsername(nickField.getText());
            }
        });
        nicknameBox.getChildren().addAll(nickLabel, nickField, confirmNick);

        // Phase 2: Totem
        totemBox = new VBox(15);
        totemBox.setAlignment(Pos.CENTER);
        totemBox.setVisible(false);
        Label totemLabel = new Label("Select your tribe totem:");
        totemLabel.setTextFill(Color.WHITE);

        FlowPane totemsPane = new FlowPane(10, 10);
        totemsPane.setAlignment(Pos.CENTER);
        for(Totem t : Totem.values()) {
            Button tBtn = new Button(t.name());
            tBtn.setPrefSize(100, 50);
            // FIX 3: Usiamo requestSelectTotem invece di handleSelectTotem
            tBtn.setOnAction(e -> controller.requestSelectTotem(t));
            totemButtons.put(t, tBtn);
            totemsPane.getChildren().add(tBtn);
        }
        totemBox.getChildren().addAll(totemLabel, totemsPane);

        centerStack.getChildren().addAll(nicknameBox, totemBox);
        root.setCenter(centerStack);

        // Right Bar: Player list
        VBox rightBar = new VBox(10);
        rightBar.setPadding(new Insets(20));
        rightBar.setPrefWidth(250);
        rightBar.setStyle("-fx-background-color: #2b1d14;");
        Label playersHeader = new Label("PLAYERS");
        playersHeader.setTextFill(Color.web("#F2D5A3"));
        playerList = new VBox(5);
        rightBar.getChildren().addAll(playersHeader, new Separator(), playerList);
        root.setRight(rightBar);

        Button leaveBtn = new Button("LEAVE LOBBY");
        leaveBtn.setStyle("-fx-base: #8B0000; -fx-text-fill: white;");
        leaveBtn.setOnAction(e -> controller.requestLeaveLobby());
        root.setBottom(leaveBtn);
        BorderPane.setMargin(leaveBtn, new Insets(20));

        return root;
    }

    public void onNicknameAccepted(String nickname) {
        this.myNickname = nickname;
        Platform.runLater(() -> {
            nicknameBox.setVisible(false);
            totemBox.setVisible(true);
        });
    }

    public void onNicknameRejected() {
        Platform.runLater(() -> {
            nickField.setEditable(true);
            nickField.setText("");
        });
    }

    public void updateLobbyState(LobbyInfo lobby) {
        Platform.runLater(() -> {
            // Update Player List
            playerList.getChildren().clear();
            Map<String, Totem> chosen = lobby.chosenTotems();

            for (String nick : lobby.currentPlayers()) {
                Totem t = chosen.get(nick);
                String info = (t != null) ? " [" + t + "]" : " (picking...)";
                Label l = new Label("• " + nick + info);
                l.setTextFill(nick.equals(myNickname) ? Color.LIME : Color.LIGHTGRAY);
                playerList.getChildren().add(l);
            }

            // Update Totem Buttons
            for (Totem t : Totem.values()) {
                Button btn = totemButtons.get(t);
                if (btn == null) continue;

                String occupant = null;
                for(Map.Entry<String, Totem> entry : chosen.entrySet()) {
                    if(entry.getValue() == t) { occupant = entry.getKey(); break; }
                }

                if (occupant != null) {
                    if (occupant.equals(myNickname)) {
                        btn.setDisable(false);
                        btn.setStyle("-fx-base: LIME; -fx-text-fill: black;");
                        btn.setText(t.name() + " (YOU)");
                    } else {
                        btn.setDisable(true);
                        btn.setStyle("-fx-base: #444; -fx-text-fill: #888;");
                        btn.setText(t.name() + "\n(" + occupant + ")");
                    }
                } else {
                    btn.setDisable(false);
                    btn.setStyle(""); // Default
                    btn.setText(t.name());
                }
            }
        });
    }
}