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

public class PlayerSidebarItem extends VBox {

    public PlayerSidebarItem(String nickname, boolean isMe, boolean isActive, boolean isOffline, boolean isViewed, int food, int pp, Totem totem, Font tribalFont, Runnable onClick) {
        super(5); // Spacing

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
            String totemPath = "/it.polimi.ingsw.am02.images/totems/totem_" + totem.name().toLowerCase() + ".png";
            ImageView totemIcon = new ImageView(ImageLoader.getImage(totemPath));
            totemIcon.setFitHeight(20);
            totemIcon.setPreserveRatio(true);
            nameRow.getChildren().add(totemIcon);
        }

        // Setup player name
        String displayName = nickname.toUpperCase() + (isMe ? " (YOU)" : "");
        Label nameLabel = new Label(displayName);
        nameLabel.setTextFill(isActive ? Color.GOLD : Color.WHITE);
        if (tribalFont != null) {
            nameLabel.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 15));
        }
        nameRow.getChildren().add(nameLabel);

        // Setup player stats
        Label statsLabel = new Label("Food: " + food + " | PP: " + pp);
        statsLabel.setTextFill(Color.LIGHTGRAY);
        if (tribalFont != null) {
            statsLabel.setFont(Font.font(tribalFont.getFamily(), 12));
        }

        this.getChildren().addAll(nameRow, statsLabel);
        this.setOnMouseClicked(e -> onClick.run());
    }
}