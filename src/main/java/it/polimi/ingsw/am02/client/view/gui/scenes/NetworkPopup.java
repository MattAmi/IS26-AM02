package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NetworkPopup {

    public record ConnectionConfig(NetworkType type, String host, int port) {}

    public static StackPane buildNode(BiConsumer<ConnectionConfig, Consumer<String>> onConnect) {
        StackPane root = new StackPane();
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(400, 450);
        root.setMaxSize(400, 450);

        Rectangle clip = new Rectangle(400, 450);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        root.setClip(clip);

        Font introFontSmall = Font.font("System", 14);
        Font introFontMedium = Font.font("System", 20);
        try {
            introFontSmall = Font.loadFont(NetworkPopup.class.getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 14);
            introFontMedium = Font.loadFont(NetworkPopup.class.getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 20);
        } catch (Exception ignored) {}

        ImageView background = new ImageView();
        try {
            var imageUrl = NetworkPopup.class.getResource("/it.polimi.ingsw.am02.images/mesos_lobby.png");
            if (imageUrl != null) {
                Image img = new Image(imageUrl.toExternalForm());
                background.setImage(img);

                double zoom = 0.5;
                double vw = img.getWidth() * zoom;
                double vh = img.getHeight() * zoom;
                background.setViewport(new Rectangle2D((img.getWidth() - vw) / 2, (img.getHeight() - vh) / 2, vw, vh));
            }
        } catch (Exception ignored) {}

        background.setFitWidth(400);
        background.setFitHeight(450);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));

        content.setStyle("-fx-background-color: transparent; " +
                "-fx-border-color: #F2D5A3; " +
                "-fx-border-width: 2; " +
                "-fx-background-radius: 20; " +
                "-fx-border-radius: 20;");

        Label title = new Label("SERVER CONNECTION");
        title.setTextFill(Color.web("#F2D5A3"));
        title.setFont(introFontMedium);

        HBox typeBox = new HBox(15);
        typeBox.setAlignment(Pos.CENTER);
        ToggleGroup group = new ToggleGroup();
        RadioButton rmiBtn = new RadioButton("RMI");
        rmiBtn.setTextFill(Color.WHITE); rmiBtn.setToggleGroup(group); rmiBtn.setSelected(true); rmiBtn.setFont(introFontSmall);
        RadioButton socketBtn = new RadioButton("SOCKET");
        socketBtn.setTextFill(Color.WHITE); socketBtn.setToggleGroup(group); socketBtn.setFont(introFontSmall);
        typeBox.getChildren().addAll(rmiBtn, socketBtn);

        TextField ipField = new TextField("127.0.0.1");
        ipField.setStyle("-fx-background-color: rgba(26, 26, 26, 0.7); -fx-text-fill: white; -fx-border-color: #F2D5A3;");
        ipField.setMaxWidth(200); ipField.setFont(introFontSmall);

        TextField portField = new TextField("1099");
        portField.setStyle("-fx-background-color: rgba(26, 26, 26, 0.7); -fx-text-fill: white; -fx-border-color: #F2D5A3;");
        portField.setMaxWidth(200); portField.setFont(introFontSmall);

        Button connectBtn = new Button("CONNECT");
        connectBtn.setPrefSize(160, 45);
        connectBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #F2D5A3;");
        connectBtn.setFont(introFontSmall);

        Label statusLbl = new Label("");
        statusLbl.setTextFill(Color.YELLOW); statusLbl.setFont(introFontSmall);

        connectBtn.setOnAction(e -> {
            try {
                NetworkType type = rmiBtn.isSelected() ? NetworkType.RMI : NetworkType.SOCKET;
                onConnect.accept(new ConnectionConfig(type, ipField.getText().trim(), Integer.parseInt(portField.getText().trim())), (msg) -> {
                    javafx.application.Platform.runLater(() -> {
                        connectBtn.setDisable(false);
                        statusLbl.setTextFill(Color.RED);
                        statusLbl.setText("FAILED");
                    });
                });
            } catch (Exception ex) { statusLbl.setText("INVALID PORT"); }
        });

        content.getChildren().addAll(title, typeBox, ipField, portField, connectBtn, statusLbl);

        root.getChildren().addAll(background, content);

        return root;
    }
}