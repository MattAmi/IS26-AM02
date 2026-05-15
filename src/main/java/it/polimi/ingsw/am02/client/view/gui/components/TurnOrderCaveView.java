package it.polimi.ingsw.am02.client.view.gui.components;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import it.polimi.ingsw.am02.common.dto.TurnOrderSlotInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.List;

public class TurnOrderCaveView extends StackPane {

    public TurnOrderCaveView(int numPlayers, List<TurnOrderSlotInfo> slots, GameModel model) {
        // 1. Immagine di sfondo (la tessera grotta)
        int playersCount = Math.max(2, numPlayers);
        ImageView caveBg = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/turn_order_tiles/tile_turn_" + playersCount + "p.png"));
        caveBg.setFitHeight(150);
        caveBg.setPreserveRatio(true);
        this.getChildren().add(caveBg);

        // 2. Contenitore verticale per i totem
        // Regola lo spacing (es: 10) per la distanza tra un cerchio e l'altro
        VBox totemSlots = new VBox(5);
        totemSlots.setAlignment(Pos.TOP_CENTER);

        // REGOLA IL PRIMO NUMERO (es: 25) per centrare il primo totem sul cerchio in alto
        totemSlots.setPadding(new Insets(25-((numPlayers-2)*5), 0, 0, 0));

        // Blocchiamo l'altezza per evitare che la VBox si restringa
        totemSlots.setPrefHeight(150);
        totemSlots.setMaxHeight(150);

        if (slots != null) {
            for (TurnOrderSlotInfo slot : slots) {
                if (slot.occupantNickname() != null) {
                    Totem totem = model.getTotem(slot.occupantNickname());
                    if (totem != null) {
                        String path = "/it.polimi.ingsw.am02.images/totems/totem_" + totem.name().toLowerCase() + ".png";
                        ImageView tView = new ImageView(ImageLoader.getImage(path));

                        // REGOLA QUESTA ALTEZZA per far stare il totem nel cerchio
                        double totemHeight = 22;
                        tView.setFitHeight(totemHeight);
                        tView.setPreserveRatio(true);
                        tView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 3, 0, 0, 1);");

                        totemSlots.getChildren().add(tView);
                    }
                } else {
                    // SE LO SLOT È VUOTO: Aggiungiamo un "fantasma" invisibile
                    // Deve avere la stessa altezza del totem per non far saltare la fila
                    Region spacer = new Region();
                    spacer.setPrefHeight(22);
                    spacer.setMinHeight(22);
                    totemSlots.getChildren().add(spacer);
                }
            }
        }

        this.getChildren().add(totemSlots);
    }
}