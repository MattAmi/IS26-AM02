package it.polimi.ingsw.am02.client.view.gui;

import javafx.application.Application;
import javafx.stage.Stage;

// Ecco l'import fondamentale! Dice al MainGUI dove trovare la LoginScene
import it.polimi.ingsw.am02.client.view.gui.scenes.LoginScene;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Creiamo un'istanza della tua LoginScene
        LoginScene loginScene = new LoginScene();

        // 2. Costruiamo la scena e la inseriamo nel palcoscenico principale (Stage)
        primaryStage.setScene(loginScene.buildScene());

        // 3. Impostiamo il titolo della finestra e le dimensioni fisse
        primaryStage.setTitle("Mesos");
        primaryStage.setResizable(false); // Blocca il ridimensionamento della finestra

        // 4. Mostriamo lo spettacolo!
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Questo comando avvia l'intera applicazione JavaFX
        launch(args);
    }
}