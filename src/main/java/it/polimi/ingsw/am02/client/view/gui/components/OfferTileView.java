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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class OfferTileView extends StackPane {

    public OfferTileView(OfferTileInfo info, Totem occupantTotem, GuiController controller) {
        this.setCursor(Cursor.HAND);

        // Sfondo tessera
        ImageView bgImage = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/offer_tiles/tile_offer_" + info.tileID() + ".png"));
        bgImage.setFitHeight(150);
        bgImage.setPreserveRatio(true);
        this.getChildren().add(bgImage);

        // Rettangolo dorato per l'Hover (lo slot)
        Rectangle slotHighlight = new Rectangle(50, 45);
        slotHighlight.setFill(Color.TRANSPARENT);
        slotHighlight.setStroke(Color.GOLD);
        slotHighlight.setStrokeWidth(3);
        slotHighlight.setOpacity(0);
        slotHighlight.setMouseTransparent(true); // Non blocca i click

        StackPane.setAlignment(slotHighlight, Pos.TOP_CENTER);
        StackPane.setMargin(slotHighlight, new Insets(20, 0, 0, 0));

        // Attiviamo l'hover sulla tessera
        this.setOnMouseEntered(e -> slotHighlight.setOpacity(1));
        this.setOnMouseExited(e -> slotHighlight.setOpacity(0));
        this.getChildren().add(slotHighlight);

        // Posizionamento Totem
        if (occupantTotem != null) {
            String path = "/it.polimi.ingsw.am02.images/totems/totem_" + occupantTotem.name().toLowerCase() + ".png";
            ImageView totemView = new ImageView(ImageLoader.getImage(path));

            // Regola l'altezza del totem sulla tessera
            totemView.setFitHeight(40);
            totemView.setPreserveRatio(true);
            totemView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 5, 0, 0, 2);");

            StackPane.setAlignment(totemView, Pos.TOP_CENTER);

            // REGOLA IL PRIMO NUMERO (es: 30) per abbassare il totem nello slot dorato
            StackPane.setMargin(totemView, new Insets(15, 0, 0, 0));

            this.getChildren().add(totemView);
        }

        this.setOnMouseClicked(e -> controller.moveTotem(info.tileID()));
    }
}