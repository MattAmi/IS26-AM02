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

/**
 * Graphical representation of the turn order cave in the GUI.
 * This class displays the cave tile and the totems in their respective slots.
 */
public class TurnOrderCaveView extends StackPane {

    /**
     * Configuration object for defining the layout of the cave based on player count.
     */
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

    /**
     * Returns the layout configuration for a specific number of players.
     *
     * @param numPlayers The number of players in the game.
     * @return The CaveLayoutConfig instance.
     */
    private CaveLayoutConfig getLayoutForPlayers(int numPlayers) {

        // If a specific tile looks slightly off in the future, just tweak its case!
        return switch (numPlayers) {
            case 5 -> new CaveLayoutConfig(10, 5, 22); // 25 - (3*4)
            case 4 -> new CaveLayoutConfig(20, 5, 22); // 25 - (2*4)
            case 3 -> new CaveLayoutConfig(25, 5, 22); // 25 - (1*4)
            default -> new CaveLayoutConfig(33, 5, 22); // 25 - (0*4)
        };
    }

    /**
     * Constructs a new TurnOrderCaveView.
     *
     * @param numPlayers The number of players in the game.
     * @param slots      The list of turn order slots information.
     * @param model      The game model to retrieve player totems.
     */
    public TurnOrderCaveView(int numPlayers, List<TurnOrderSlotInfo> slots, GameModel model) {

        int playersCount = Math.max(2, Math.min(numPlayers, 5));

        CaveLayoutConfig config = getLayoutForPlayers(playersCount);

        ImageView caveBg = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/turn_order_tiles/tile_turn_" + playersCount + "p.png"));
        caveBg.setFitHeight(150);
        caveBg.setPreserveRatio(true);
        this.getChildren().add(caveBg);

        VBox totemSlots = new VBox(config.spacing);
        totemSlots.setAlignment(Pos.TOP_CENTER);

        totemSlots.setPadding(new Insets(config.topPadding, 0, 0, 0));

        totemSlots.setPrefHeight(150);
        totemSlots.setMaxHeight(150);

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

        if (slots != null) {
            for (TurnOrderSlotInfo slot : slots) {
                if (slot.occupantNickname() != null) {
                    Totem totem = model.getTotem(slot.occupantNickname());
                    if (totem != null) {
                        String path = "/it.polimi.ingsw.am02.images/totems/totem_" + totem.name().toLowerCase() + ".png";
                        ImageView tView = new ImageView(ImageLoader.getImage(path));

                        tView.setFitHeight(config.totemHeight);
                        tView.setPreserveRatio(true);
                        tView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 3, 0, 0, 1);");

                        totemSlots.getChildren().add(tView);
                    }
                } else {
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