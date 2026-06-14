package it.polimi.ingsw.am02.client.view.gui.components;

import it.polimi.ingsw.am02.client.view.CardCatalog;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Graphical representation of a game card in the GUI.
 * This class handles the display, selection effect, and tooltip for a card.
 */
public class GameCardView extends VBox {

    private final ImageView cardImageView;

    /** The card's resting translateY within its cascade, captured on first hover so the lift is relative. */
    private double baseTranslateY = 0;
    /** Whether the pointer is currently over the card, guarding against re-capturing the base position. */
    private boolean hovered = false;

    /**
     * Constructs a new GameCardView.
     *
     * @param cardId     The unique identifier of the card.
     * @param isSelected Whether the card is currently selected.
     * @param height     The desired height for the card image.
     * @param onClick    The action to perform when the card is clicked.
     */
    public GameCardView(String cardId, boolean isSelected, double height, Runnable onClick) {
        this.setCursor(Cursor.HAND);

        String path = getCardPath(cardId);
        this.cardImageView = new ImageView(ImageLoader.getImage(path));
        this.cardImageView.setFitHeight(height);
        this.cardImageView.setPreserveRatio(true);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(12);
        clip.setArcHeight(12);

        clip.setWidth(this.cardImageView.getBoundsInLocal().getWidth());
        clip.setHeight(this.cardImageView.getBoundsInLocal().getHeight());

        this.cardImageView.boundsInLocalProperty().addListener((obs, oldVal, newVal) -> {
            clip.setWidth(newVal.getWidth());
            clip.setHeight(newVal.getHeight());
        });
        this.cardImageView.setClip(clip);

        this.getChildren().add(cardImageView);

        if (isSelected) {
            this.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);");
        }

        try {
            Tooltip tooltip = new Tooltip(CardCatalog.getInstance().getFullDescription(cardId));
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(360);
            tooltip.setShowDelay(Duration.seconds(0.4));
            tooltip.setShowDuration(Duration.INDEFINITE);
            Tooltip.install(this, tooltip);
        } catch (Exception ignored) { }

        this.setOnMouseEntered(e -> {
            if (!hovered) {
                baseTranslateY = this.getTranslateY();
                hovered = true;
            }
            this.setTranslateY(baseTranslateY - 10);
            this.setScaleX(1.1);
            this.setScaleY(1.1);
            // Bring the hovered card above its neighbours WITHOUT reordering the
            // StackPane's children: toFront() mutates the child list mid-dispatch,
            // which re-fires enter/exit and makes the cards jitter. A negative
            // viewOrder renders this node on top while leaving the list untouched.
            this.setViewOrder(-1);
            if (!isSelected) {
                this.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.7), 15, 0.4, 0, 0);");
            }
        });

        this.setOnMouseExited(e -> {
            this.setTranslateY(baseTranslateY);
            this.setScaleX(1.0);
            this.setScaleY(1.0);
            this.setViewOrder(0);
            hovered = false;
            this.setStyle(isSelected ? "-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);" : "");
        });

        this.setOnMouseClicked(e -> onClick.run());
    }

    /**
     * Returns the ImageView containing the card's graphic.
     *
     * @return The ImageView instance.
     */
    public ImageView getCardImageView() {
        return cardImageView;
    }

    /**
     * Resolves the resource path for a card image based on its ID.
     *
     * @param id The card identifier.
     * @return The resource path string.
     */
    private String getCardPath(String id) {
        String subDir = id.startsWith("C_") ? "characters/" : id.startsWith("B_") ? "buildings/" : "events/";
        return "/images/cards/" + subDir + id + ".png";
    }
}