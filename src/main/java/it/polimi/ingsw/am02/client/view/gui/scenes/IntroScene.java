package it.polimi.ingsw.am02.client.view.gui.scenes;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class IntroScene {

    private String customFontFamily = "System";

    public Scene buildScene(Runnable onFinished) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        String fontUrl = getClass().getResource("/it.polimi.ingsw.am02.fonts/intro.ttf").toExternalForm();
        Font baseFont = Font.loadFont(fontUrl, 25);
        if (baseFont != null) {
            customFontFamily = baseFont.getFamily();
        }

        String introText = "Thousands of years ago, a new era was beginning for humankind.\n\n" +
                "The nomadic hunter-gatherers who had laboriously earned their place on Earth organized into small" +
                "groups, differentiating social roles, building the first settlements, and initiating a great revolution.\n\n" +
                "Scientists call this period Mesolithic, and this game talks about those people.\n" +
                "Step into the role of a tribal leader, carefully choose the tasks to entrust to the people joining your tribe," +
                "construct specialized buildings, and prepare wisely for the events you will face" +
                "guiding your tribe to victory!";

        Label textLabel = new Label(introText);
        textLabel.setFont(Font.font(customFontFamily, 25));
        textLabel.setTextFill(Color.web("#F2D5A3"));
        textLabel.setTextAlignment(TextAlignment.CENTER);
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(900);
        textLabel.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(4), textLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition stay = new PauseTransition(Duration.seconds(20));

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), textLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, stay, fadeOut);

        sequence.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });

        root.getChildren().add(textLabel);
        StackPane.setAlignment(textLabel, Pos.CENTER);

        root.setOnMouseClicked(e -> {
            sequence.stop();
            if (onFinished != null) onFinished.run();
        });

        sequence.play();

        return new Scene(root, 1280, 720);
    }
}