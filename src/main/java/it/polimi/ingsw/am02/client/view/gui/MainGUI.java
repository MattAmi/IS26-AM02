package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.view.gui.scenes.IntroScene;
import it.polimi.ingsw.am02.client.view.gui.scenes.LobbyListScene;
import it.polimi.ingsw.am02.client.view.gui.scenes.LobbyScene;
import javafx.application.Application;
import javafx.stage.Stage;

// Ecco l'import fondamentale! Dice al MainGUI dove trovare la LoginScene
import it.polimi.ingsw.am02.client.view.gui.scenes.LoginScene;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {

        IntroScene loginScene = new IntroScene();
        //PROVA

        primaryStage.setScene(loginScene.buildScene());

        primaryStage.setTitle("MESOS");
        primaryStage.setResizable(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        // Questo comando avvia l'intera applicazione JavaFX
        launch(args);
    }
}