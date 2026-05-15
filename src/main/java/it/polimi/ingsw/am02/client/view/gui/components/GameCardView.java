package it.polimi.ingsw.am02.client.view.gui.components;

import it.polimi.ingsw.am02.client.view.CardCatalog;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class GameCardView extends VBox {

    private final ImageView cardImageView;

    public GameCardView(String cardId, boolean isSelected, double height, Runnable onClick) {
        this.setCursor(Cursor.HAND);

        String path = getCardPath(cardId);
        this.cardImageView = new ImageView(ImageLoader.getImage(path));
        this.cardImageView.setFitHeight(height);
        this.cardImageView.setPreserveRatio(true);

        if (isSelected) {
            this.cardImageView.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);");
        }

        this.getChildren().add(cardImageView);

        // Attach tooltip with card rules
        try {
            Tooltip tooltip = new Tooltip(CardCatalog.getInstance().format(cardId));
            tooltip.setWrapText(true);
            tooltip.setPrefWidth(250);
            tooltip.setShowDelay(Duration.seconds(1));
            Tooltip.install(this, tooltip);
        } catch (Exception ignored) { }

        // Hover animations
        this.setOnMouseEntered(e -> {
            cardImageView.setTranslateY(-10);
            cardImageView.setScaleX(1.1);
            cardImageView.setScaleY(1.1);
            if (!isSelected) {
                cardImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.7), 15, 0.4, 0, 0);");
            }
        });

        this.setOnMouseExited(e -> {
            cardImageView.setTranslateY(0);
            cardImageView.setScaleX(1.0);
            cardImageView.setScaleY(1.0);
            cardImageView.setStyle(isSelected ? "-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);" : "");
        });

        this.setOnMouseClicked(e -> onClick.run());
    }

    public ImageView getCardImageView() {
        return cardImageView;
    }

    private String getCardPath(String id) {
        String subDir = id.startsWith("C_") ? "characters/" : id.startsWith("B_") ? "buildings/" : "events/";
        return "/it.polimi.ingsw.am02.images/cards/" + subDir + id + ".png";
    }
}