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

    // --- OOP Configuration Object ---
    private static class CaveLayoutConfig {
        final double topPadding;
        final double spacing;
        final double totemHeight;

        CaveLayoutConfig(double topPadding, double spacing, double totemHeight) {
            this.topPadding = topPadding;
            this.spacing = spacing;
            this.totemHeight = totemHeight;
        }
    }

    // --- Factory method: Maps the exact pixels based on your previous formula ---
    private CaveLayoutConfig getLayoutForPlayers(int numPlayers) {

        // If a specific tile looks slightly off in the future, just tweak its case!
        return switch (numPlayers) {
            case 5 -> new CaveLayoutConfig(10, 5, 22); // 25 - (3*4)
            case 4 -> new CaveLayoutConfig(20, 5, 22); // 25 - (2*4)
            case 3 -> new CaveLayoutConfig(25, 5, 22); // 25 - (1*4)
            default -> new CaveLayoutConfig(33, 5, 22); // 25 - (0*4)
        };
    }

    public TurnOrderCaveView(int numPlayers, List<TurnOrderSlotInfo> slots, GameModel model) {

        // Cap to max players (assuming 5 is your max based on the formula)
        int playersCount = Math.max(2, Math.min(numPlayers, 5));

        // Fetch layout config for THIS specific tile
        CaveLayoutConfig config = getLayoutForPlayers(playersCount);

        // 1. Background image (cave tile)
        ImageView caveBg = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/turn_order_tiles/tile_turn_" + playersCount + "p.png"));
        caveBg.setFitHeight(150);
        caveBg.setPreserveRatio(true);
        this.getChildren().add(caveBg);

        // 2. Vertical container for totems using config spacing
        VBox totemSlots = new VBox(config.spacing);
        totemSlots.setAlignment(Pos.TOP_CENTER);

        // Apply specific top padding from config instead of inline math
        totemSlots.setPadding(new Insets(config.topPadding, 0, 0, 0));

        // Lock height to prevent VBox from shrinking and causing layout shifts
        totemSlots.setPrefHeight(150);
        totemSlots.setMaxHeight(150);

        // --- ARROTONDAMENTO ANGOLI TESSERA ---
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(10);
        clip.setArcHeight(10);

        clip.setWidth(caveBg.getBoundsInLocal().getWidth());
        clip.setHeight(caveBg.getBoundsInLocal().getHeight());

        caveBg.boundsInLocalProperty().addListener((obs, oldVal, newVal) -> {
            clip.setWidth(newVal.getWidth());
            clip.setHeight(newVal.getHeight());
        });
        caveBg.setClip(clip);
// --------------------------------------

        if (slots != null) {
            for (TurnOrderSlotInfo slot : slots) {
                if (slot.occupantNickname() != null) {
                    Totem totem = model.getTotem(slot.occupantNickname());
                    if (totem != null) {
                        String path = "/it.polimi.ingsw.am02.images/totems/totem_" + totem.name().toLowerCase() + ".png";
                        ImageView tView = new ImageView(ImageLoader.getImage(path));

                        // Apply config height
                        tView.setFitHeight(config.totemHeight);
                        tView.setPreserveRatio(true);
                        tView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 3, 0, 0, 1);");

                        totemSlots.getChildren().add(tView);
                    }
                } else {
                    // EMPTY SLOT: Add an invisible ghost spacer matching config height
                    Region spacer = new Region();
                    spacer.setPrefHeight(config.totemHeight);
                    spacer.setMinHeight(config.totemHeight);
                    totemSlots.getChildren().add(spacer);
                }
            }
        }

        this.getChildren().add(totemSlots);
    }
}