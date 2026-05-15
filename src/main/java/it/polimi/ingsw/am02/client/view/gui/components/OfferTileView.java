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

public class OfferTileView extends StackPane {

    public OfferTileView(OfferTileInfo info, Totem occupantTotem, GuiController controller) {
        this.setCursor(Cursor.HAND);

        // Sfondo tessera
        ImageView bgImage = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/offer_tiles/tile_offer_" + info.tileID() + ".png"));
        bgImage.setFitHeight(150);
        bgImage.setPreserveRatio(true);
        this.getChildren().add(bgImage);

        if (occupantTotem != null) {
            String path = "/it.polimi.ingsw.am02.images/totems/totem_" + occupantTotem.name().toLowerCase() + ".png";
            ImageView totemView = new ImageView(ImageLoader.getImage(path));

            // Regola l'altezza del totem sulla tessera
            totemView.setFitHeight(40);
            totemView.setPreserveRatio(true);
            totemView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 5, 0, 0, 2);");

            StackPane.setAlignment(totemView, Pos.TOP_CENTER);

            // Abbassa il totem nello slot
            StackPane.setMargin(totemView, new Insets(15, 0, 0, 0));

            this.getChildren().add(totemView);
        }

        this.setOnMouseClicked(e -> controller.moveTotem(info.tileID()));
    }
}