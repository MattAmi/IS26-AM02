package it.polimi.ingsw.am02.client.view.gui.scenes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class SummaryCardOverlay {

    private boolean showingFront = true;

    /**
     * Costruisce il nodo dell'overlay carta riepilogo.
     * Da chiamare da GuiController e inserire nel modalLayer.
     *
     * @param onClose callback per chiudere l'overlay
     * @return il nodo pronto da aggiungere al modalLayer
     */
    public VBox buildNode(Runnable onClose) {

        Image frontImage = new Image(
                getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/summary_card_front.png")
        );
        Image backImage = new Image(
                getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/summary_card_back.png")
        );

        ImageView cardView = new ImageView(frontImage);
        cardView.setPreserveRatio(true);
        cardView.setFitHeight(520);

        // Bottone flip
        Button flipBtn = new Button("↩ Rotate card");
        flipBtn.setStyle(
                "-fx-background-color: #5C6B32; -fx-text-fill: white;" +
                        "-fx-font-size: 13; -fx-padding: 8 20; -fx-background-radius: 6;"
        );
        flipBtn.setOnAction(e -> {
            showingFront = !showingFront;
            cardView.setImage(showingFront ? frontImage : backImage);
            flipBtn.setText(showingFront ? "↩ Rotate card" : "↩ Rotate card");
        });

        // Bottone chiudi
        Button closeBtn = new Button("✕ close");
        closeBtn.setStyle(
                "-fx-background-color: #6B2A2A; -fx-text-fill: white;" +
                        "-fx-font-size: 13; -fx-padding: 8 20; -fx-background-radius: 6;"
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