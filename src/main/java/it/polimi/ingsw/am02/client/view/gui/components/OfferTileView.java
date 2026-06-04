package it.polimi.ingsw.am02.client.view.gui.components;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Graphical representation of an offer tile in the GUI.
 * This class displays the tile background and the totem of the occupant player if any.
 */
public class OfferTileView extends StackPane {

    /**
     * Constructs a new OfferTileView.
     *
     * @param info           Information about the offer tile.
     * @param occupantTotem  The totem of the player occupying the tile, or null if empty.
     * @param controller     The GUI controller for handling tile clicks.
     */
    public OfferTileView(OfferTileInfo info, Totem occupantTotem, GuiController controller) {
        this.setCursor(Cursor.HAND);

        ImageView bgImage = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/offer_tiles/tile_offer_" + info.tileID() + ".png"));
        bgImage.setFitHeight(150);
        bgImage.setPreserveRatio(true);
        this.getChildren().add(bgImage);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(10);
        clip.setArcHeight(10);

        clip.setWidth(bgImage.getBoundsInLocal().getWidth());
        clip.setHeight(bgImage.getBoundsInLocal().getHeight());

        bgImage.boundsInLocalProperty().addListener((obs, oldVal, newVal) -> {
            clip.setWidth(newVal.getWidth());
            clip.setHeight(newVal.getHeight());
        });
        bgImage.setClip(clip);

        if (occupantTotem != null) {
            String path = "/it.polimi.ingsw.am02.images/totems/totem_" + occupantTotem.name().toLowerCase() + ".png";
            ImageView totemView = new ImageView(ImageLoader.getImage(path));

            totemView.setFitHeight(40);
            totemView.setPreserveRatio(true);
            totemView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 5, 0, 0, 2);");

            StackPane.setAlignment(totemView, Pos.TOP_CENTER);

            StackPane.setMargin(totemView, new Insets(15, 0, 0, 0));

            this.getChildren().add(totemView);
        }

        this.setOnMouseClicked(e -> controller.moveTotem(info.tileID()));
    }
}