package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NetworkPopup {

    public record ConnectionConfig(NetworkType type, String host, int port) {}

    /**
     * @param onConnect Callback che riceve i dati (Config) e una seconda funzione (onError)
     * da chiamare se la connessione fallisce per aggiornare la UI.
     */
    public static VBox buildNode(BiConsumer<ConnectionConfig, Consumer<String>> onConnect) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setMaxSize(400, 350);
        root.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 2px; -fx-background-radius: 10px; -fx-border-radius: 10px;");

        Label title = new Label("SERVER CONNECTION");
        title.setTextFill(Color.web("#F2D5A3"));
        title.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));

        // Network Type Toggle
        HBox typeBox = new HBox(15);
        typeBox.setAlignment(Pos.CENTER);
        ToggleGroup group = new ToggleGroup();

        RadioButton rmiBtn = new RadioButton("RMI");
        rmiBtn.setTextFill(Color.WHITE); rmiBtn.setToggleGroup(group); rmiBtn.setSelected(true);

        RadioButton socketBtn = new RadioButton("SOCKET");
        socketBtn.setTextFill(Color.WHITE); socketBtn.setToggleGroup(group);
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

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == rmiBtn) portField.setText("1099");
            else portField.setText("1100");
        });

        Button connectBtn = new Button("CONNECT");
        connectBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-font-weight: bold;");
        connectBtn.setPrefSize(140, 40);

        // La label degli errori direttamente nel form!
        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setAlignment(Pos.CENTER);

        connectBtn.setOnAction(e -> {
            NetworkType type = rmiBtn.isSelected() ? NetworkType.RMI : NetworkType.SOCKET;
            String ip = ipField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                port = type == NetworkType.RMI ? 1099 : 1100;
            }

            connectBtn.setDisable(true);
            statusLbl.setTextFill(Color.YELLOW);
            statusLbl.setText("Connessione in corso...");

            // Chiamiamo il Controller passando i dati e la funzione per gestire l'errore
            onConnect.accept(new ConnectionConfig(type, ip, port), (errorMessage) -> {
                // Se fallisce, questa lambda viene eseguita dal Controller!
                javafx.application.Platform.runLater(() -> {
                    connectBtn.setDisable(false); // Riabilita il bottone
                    statusLbl.setTextFill(Color.RED);
                    statusLbl.setText("Connessione fallita. Riprova.");
                });
            });
        });

        root.getChildren().addAll(title, typeBox, ipField, portField, connectBtn, statusLbl);
        return root;
    }
}