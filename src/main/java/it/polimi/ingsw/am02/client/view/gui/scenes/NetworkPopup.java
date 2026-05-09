package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworkPopup {

    /**
     * Data record to hold the connection parameters.
     */
    public record ConnectionConfig(NetworkType type, String host, int port) {}

    public static ConnectionConfig displayAndChoose() {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UNDECORATED);

        ConnectionConfig[] result = new ConnectionConfig[1];

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle(
                "-fx-background-color: rgba(43, 29, 20, 0.95); " +
                        "-fx-border-color: #F2D5A3; " +
                        "-fx-border-width: 3px; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-border-radius: 15px;"
        );

        Label title = new Label("SERVER CONNECTION");
        title.setTextFill(Color.web("#F2D5A3"));
        title.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 22));

        // Network Type Toggle
        HBox typeBox = new HBox(15);
        typeBox.setAlignment(Pos.CENTER);
        ToggleGroup group = new ToggleGroup();
        RadioButton rmiBtn = new RadioButton("RMI");
        rmiBtn.setTextFill(Color.WHITE);
        rmiBtn.setToggleGroup(group);
        rmiBtn.setSelected(true); // Default

        RadioButton socketBtn = new RadioButton("SOCKET");
        socketBtn.setTextFill(Color.WHITE);
        socketBtn.setToggleGroup(group);
        typeBox.getChildren().addAll(rmiBtn, socketBtn);

        // Inputs
        TextField ipField = new TextField("127.0.0.1");
        ipField.setPromptText("Server IP");
        ipField.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        ipField.setMaxWidth(200);

        TextField portField = new TextField("1099");
        portField.setPromptText("Server Port");
        portField.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        portField.setMaxWidth(200);

        // Auto-update default port based on radio button selection
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == rmiBtn) portField.setText("1099");
            else portField.setText("1100");
        });

        // Connect Button
        Button connectBtn = new Button("CONNECT");
        connectBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-font-weight: bold;");
        connectBtn.setPrefSize(140, 40);
        connectBtn.setOnAction(e -> {
            NetworkType type = rmiBtn.isSelected() ? NetworkType.RMI : NetworkType.SOCKET;
            String ip = ipField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                port = type == NetworkType.RMI ? 1099 : 1100; // fallback
            }
            result[0] = new ConnectionConfig(type, ip, port);
            popupStage.close();
        });

        root.getChildren().addAll(title, typeBox, ipField, portField, connectBtn);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);
        popupStage.centerOnScreen();
        popupStage.showAndWait();

        // Fallback if the user somehow closes the window
        if (result[0] == null) {
            result[0] = new ConnectionConfig(NetworkType.RMI, "127.0.0.1", 1099);
        }
        return result[0];
    }
}