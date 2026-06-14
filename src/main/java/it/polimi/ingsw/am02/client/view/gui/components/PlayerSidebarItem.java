package it.polimi.ingsw.am02.client.view.gui.components;

import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Represents an item in the player sidebar of the GUI.
 * This class displays player information such as nickname, totem, food, and prestige points.
 */
public class PlayerSidebarItem extends VBox {

    /**
     * Constructs a new PlayerSidebarItem.
     *
     * @param nickname    The player's nickname.
     * @param isMe        True if this item represents the local player.
     * @param isActive    True if it is currently this player's turn.
     * @param isOffline   True if the player is currently disconnected.
     * @param isViewed    True if this player's hand is currently being viewed.
     * @param food        The player's current food count.
     * @param pp          The player's current prestige points.
     * @param picksUp     Remaining upper-row picks (shown only in ACTION_RESOLUTION; 0 otherwise).
     * @param picksLow    Remaining lower-row picks (shown only in ACTION_RESOLUTION; 0 otherwise).
     * @param totem       The totem color assigned to the player.
     * @param tribalFont  The font to use for text display.
     * @param onClick     The action to perform when the item is clicked.
     */
    public PlayerSidebarItem(String nickname, boolean isMe, boolean isActive, boolean isOffline, boolean isViewed, int food, int pp, int picksUp, int picksLow, Totem totem, Font tribalFont, Runnable onClick) {
        super(5);

        this.setPadding(new Insets(10));
        this.setCursor(Cursor.HAND);

        // Dynamic styling based on player state
        String borderColor = isViewed ? "#F2D5A3" : "transparent";
        String bgColor = isActive ? "rgba(163, 29, 29, 0.3)" : "rgba(255,255,255,0.05)";

        this.setStyle("-fx-background-color: " + bgColor +
                "; -fx-border-color: " + borderColor +
                "; -fx-border-radius: 5; -fx-border-width: 2;");
        this.setOpacity(isOffline ? 0.5 : 1.0);

        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        // Load and add Totem icon if present
        if (totem != null) {
            String totemPath = "/images/totems/totem_" + totem.name().toLowerCase() + ".png";
            ImageView totemIcon = new ImageView(ImageLoader.getImage(totemPath));
            totemIcon.setFitHeight(20);
            totemIcon.setPreserveRatio(true);
            nameRow.getChildren().add(totemIcon);
        }

        // Setup player name
        String displayName = nickname + (isMe ? " (YOU)" : "");
        Label nameLabel = new Label(displayName);
        nameLabel.setTextFill(isActive ? Color.GOLD : Color.WHITE);
        if (tribalFont != null) {
            nameLabel.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 15));
        }
        nameRow.getChildren().add(nameLabel);

        // Setup player stats
        HBox statsRow = new HBox(10);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        HBox foodBox = new HBox(4);
        foodBox.setAlignment(Pos.CENTER_LEFT);
        ImageView foodIcon = new ImageView(ImageLoader.getImage("/images/icons/food_point.png"));
        foodIcon.setFitHeight(12);
        foodIcon.setPreserveRatio(true);
        Label foodLabel = new Label(String.valueOf(food));
        foodLabel.setTextFill(Color.LIGHTGRAY);
        if (tribalFont != null) {
            foodLabel.setFont(Font.font(tribalFont.getFamily(), 12));
        }
        foodBox.getChildren().addAll(foodIcon, foodLabel);

        HBox ppBox = new HBox(4);
        ppBox.setAlignment(Pos.CENTER_LEFT);
        ImageView ppIcon = new ImageView(ImageLoader.getImage("/images/icons/prestige_point.png"));
        ppIcon.setFitHeight(12);
        ppIcon.setPreserveRatio(true);
        Label ppLabel = new Label(String.valueOf(pp));
        ppLabel.setTextFill(Color.LIGHTGRAY);
        if (tribalFont != null) {
            ppLabel.setFont(Font.font(tribalFont.getFamily(), 12));
        }
        ppBox.getChildren().addAll(ppIcon, ppLabel);

        // Remaining pickable cards (upper/lower row), mirroring the TUI's
        // "Picks (Up/Low)". Drawn as a green up-arrow and a red down-arrow each
        // followed by their count, placed right after the prestige points.
        HBox picksBox = new HBox(6);
        picksBox.setAlignment(Pos.CENTER_LEFT);
        picksBox.getChildren().addAll(
                buildPickBadge("/images/icons/pick_up.png", picksUp, tribalFont),
                buildPickBadge("/images/icons/pick_down.png", picksLow, tribalFont)
        );

        statsRow.getChildren().addAll(foodBox, ppBox, picksBox);

        this.getChildren().addAll(nameRow, statsRow);
        this.setOnMouseClicked(e -> onClick.run());
    }

    /**
     * Builds a small "arrow + count" badge for the remaining pickable cards.
     *
     * @param iconPath   resource path of the up/down arrow icon
     * @param count      remaining picks for that row
     * @param tribalFont the font to use, or {@code null} for the default
     * @return an HBox containing the arrow icon and its count
     */
    private HBox buildPickBadge(String iconPath, int count, Font tribalFont) {
        HBox box = new HBox(3);
        box.setAlignment(Pos.CENTER_LEFT);
        ImageView icon = new ImageView(ImageLoader.getImage(iconPath));
        icon.setFitHeight(13);
        icon.setPreserveRatio(true);
        Label label = new Label(String.valueOf(count));
        label.setTextFill(count > 0 ? Color.web("#F2D5A3") : Color.GRAY);
        if (tribalFont != null) {
            label.setFont(Font.font(tribalFont.getFamily(), 12));
        }
        box.getChildren().addAll(icon, label);
        return box;
    }
}