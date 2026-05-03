package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.gui.scenes.IntroScene;
import it.polimi.ingsw.am02.client.view.gui.scenes.LobbyListScene;
import it.polimi.ingsw.am02.client.view.gui.scenes.LobbyScene;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.application.Application;
import javafx.stage.Stage;

import it.polimi.ingsw.am02.client.view.gui.scenes.LoginScene;

public class MainGUI extends Application {

    public static LobbyModel lobbyModel;
    public static NetworkType networkType;
    public static String host;
    public static int port;

    @Override
    public void start(Stage primaryStage) {

        IntroScene introScene = new IntroScene();
        // PROVA: Passiamo un finto "comportamento" usando le freccette () -> {}
        // Questo dice a Java: "Quando la scena ha finito, stampa questo messaggio"
        primaryStage.setScene(introScene.buildScene(() -> {
            System.out.println("✅ INTRO FINITA! Il bottone funziona!");
        }));

        primaryStage.setTitle("MESOS");
        primaryStage.setResizable(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}