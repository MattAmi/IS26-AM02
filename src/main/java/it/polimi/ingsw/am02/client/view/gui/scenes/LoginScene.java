package it.polimi.ingsw.am02.client.view.gui.scenes;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class LoginScene {
    private Runnable onFinished;

    public Scene buildScene(Runnable onFinished) {

        this.onFinished = onFinished;
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        ImageView bgImageView = new ImageView();

        Image bgImage = new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/mesos_box.png"));
        bgImageView.setImage(bgImage);
        bgImageView.setPreserveRatio(true);

        bgImageView.fitWidthProperty().bind(root.widthProperty().add(100));

        TranslateTransition move = new TranslateTransition(Duration.seconds(15), bgImageView);
        move.setFromX(-20);
        move.setToX(20);
        move.setFromY(-30);
        move.setToY(30);
        move.setAutoReverse(true);
        move.setCycleCount(TranslateTransition.INDEFINITE);
        move.play();

        ScaleTransition zoom = new ScaleTransition(Duration.seconds(20), bgImageView);
        zoom.setFromX(1.0);
        zoom.setFromY(1.0);
        zoom.setToX(1.05);
        zoom.setToY(1.05);
        zoom.setAutoReverse(true);
        zoom.setCycleCount(ScaleTransition.INDEFINITE);
        zoom.play();

        VBox uiPanel = new VBox();
        uiPanel.setAlignment(Pos.BOTTOM_CENTER);
        uiPanel.setPadding(new Insets(0, 0, 80, 0));

        String fontUrl = getClass().getResource("/it.polimi.ingsw.am02.fonts/tribal.ttf").toExternalForm();
        Font customFont = Font.loadFont(fontUrl, 36);

        if (customFont == null) {
            customFont = Font.font("System", FontWeight.BOLD, 36);
        }

        Label continueLabel = new Label("CLICK TO CONTINUE");
        continueLabel.setFont(customFont);
        continueLabel.setTextFill(Color.web("#F2D5A3"));
        continueLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0, 0, 0);");

        FadeTransition fade = new FadeTransition(Duration.seconds(1.2), continueLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.1);
        fade.setAutoReverse(true);
        fade.setCycleCount(FadeTransition.INDEFINITE);
        fade.play();

        root.setCursor(javafx.scene.Cursor.HAND);
        root.setOnMouseClicked(event -> handleContinue());

        uiPanel.getChildren().add(continueLabel);
        root.getChildren().addAll(bgImageView, uiPanel);

        root.setOnMouseClicked(event -> handleContinue());
        return new Scene(root, 1280, 720);
    }

    private void handleContinue() {
        if (onFinished != null) {
            onFinished.run();
        }
    }
}