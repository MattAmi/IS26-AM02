package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworkPopup {

    public static NetworkType displayAndChoose() {
        Stage popupStage = new Stage();

        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UNDECORATED);

        NetworkType[] selectedType = new NetworkType[1];
        selectedType[0] = NetworkType.SOCKET;

        Font tribalFontTitle;
        Font tribalFontButtons;
        try {
            String fontUrl = NetworkPopup.class.getResource("/it.polimi.ingsw.am02.fonts/intro.ttf").toExternalForm();
            tribalFontTitle = Font.loadFont(fontUrl, 28);
            tribalFontButtons = Font.loadFont(fontUrl, 20);
        } catch (Exception e) {
            tribalFontTitle = Font.font("System", 28);
            tribalFontButtons = Font.font("System", 20);
        }

        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        root.setStyle(
                "-fx-background-color: rgba(43, 29, 20, 0.95); " +
                        "-fx-border-color: #F2D5A3; " +
                        "-fx-border-width: 3px; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-border-radius: 15px;"
        );

        Label title = new Label("CHOOSE CONNECTION");
        title.setTextFill(Color.web("#F2D5A3"));
        if (tribalFontTitle != null) title.setFont(tribalFontTitle);

        HBox buttonsBox = new HBox(30);
        buttonsBox.setAlignment(Pos.CENTER);

        Button socketBtn = new Button("SOCKET");
        if (tribalFontButtons != null) socketBtn.setFont(tribalFontButtons);
        socketBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
        socketBtn.setPrefSize(140, 50);
        socketBtn.setOnAction(e -> {
            selectedType[0] = NetworkType.SOCKET;
            popupStage.close();
        });

        Button rmiBtn = new Button("RMI");
        if (tribalFontButtons != null) rmiBtn.setFont(tribalFontButtons);
        rmiBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
        rmiBtn.setPrefSize(140, 50);
        rmiBtn.setOnAction(e -> {
            selectedType[0] = NetworkType.RMI;
            popupStage.close();
        });

        buttonsBox.getChildren().addAll(socketBtn, rmiBtn);
        root.getChildren().addAll(title, buttonsBox);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);

        popupStage.centerOnScreen();
        popupStage.showAndWait();

        return selectedType[0];
    }
}
