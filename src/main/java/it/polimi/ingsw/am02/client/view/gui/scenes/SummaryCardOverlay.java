package it.polimi.ingsw.am02.client.view.gui.scenes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class SummaryCardOverlay {

    private boolean showingFront = true;

    public VBox buildNode(Runnable onClose) {

        Font introFont = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 13);
        if (introFont == null) introFont = Font.font("System", 13);

        Image frontImage = new Image(
                getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/summary_card_front.png")
        );
        Image backImage = new Image(
                getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/summary_card_back.png")
        );

        ImageView cardView = new ImageView(frontImage);
        cardView.setPreserveRatio(true);
        cardView.setFitHeight(520);

        Button flipBtn = new Button("Rotate card");
        flipBtn.setFont(introFont);
        flipBtn.setStyle(
                "-fx-background-color: #5C6B32; -fx-text-fill: white;" +
                        "-fx-padding: 8 20; -fx-background-radius: 6;"
        );
        flipBtn.setOnAction(e -> {
            showingFront = !showingFront;
            cardView.setImage(showingFront ? frontImage : backImage);
            flipBtn.setText(showingFront ? "Rotate card" : "Rotate card");
        });

        Button closeBtn = new Button("✕ close");
        closeBtn.setFont(introFont);
        closeBtn.setStyle(
                "-fx-background-color: #6B2A2A; -fx-text-fill: white;" +
                        "-fx-padding: 8 20; -fx-background-radius: 6;"
        );
        closeBtn.setOnAction(e -> onClose.run());

        HBox buttons = new HBox(12, flipBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox container = new VBox(16, cardView, buttons);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(24));
        container.setStyle(
                "-fx-background-color: #1C1C1C;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #F2D5A3;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 14;"
        );
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        return container;
    }
}