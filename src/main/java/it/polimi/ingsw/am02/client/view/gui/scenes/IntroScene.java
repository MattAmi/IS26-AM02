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

    // =================================================================
    // METODO 1 (SALVAVITA): buildScene() con parentesi vuote
    // =================================================================
    public Scene buildScene() {
        return buildScene(null); // Chiama il Metodo 2 in automatico passando 'null'
    }

    // =================================================================
    // METODO 2: Quello vero e proprio che costruisce la scena
    // =================================================================
    public Scene buildScene(Runnable onFinished) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        String fontUrl = getClass().getResource("/it.polimi.ingsw.am02.fonts/intro.ttf").toExternalForm();
        Font baseFont = Font.loadFont(fontUrl, 24);
        if (baseFont != null) {
            customFontFamily = baseFont.getFamily();
        }

        String introText = "Migliaia di anni fa, una nuova era stava iniziando per il genere umano.\n\n" +
                "I cacciatori-raccoglitori nomadi che avevano faticosamente guadagnato\n" +
                "il loro posto sulla terra si organizzarono in piccoli gruppi differenziando ruoli sociali,\n" +
                "costruirono i primi insediamenti e diedero avvio a una grande rivoluzione.\n\n" +
                "Gli studiosi chiamano questo periodo Mesolitico, e questo gioco tratta di quelle persone.\n\n" +
                "Vesti i panni di un capo tribale, scegli attentamente che lavori affidare alle persone\n" +
                "che si uniranno alla tua tribu', costruisci edifici specializzati e preparati con saggezza\n" +
                "per gli eventi che la tua tribu' dovra' affrontare, guidandola alla vittoria!";

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