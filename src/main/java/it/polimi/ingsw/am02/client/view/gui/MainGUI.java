package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI extends Application {

    public static LobbyModel lobbyModel;
    public static NetworkType networkType;
    public static String host;
    public static int port;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Fallback per test isolati: se premi "Run" direttamente su MainGUI
            // senza passare da ClientApp, mettiamo dei valori di base per non crashare.
            if (lobbyModel == null) {
                lobbyModel = new LobbyModel();
                networkType = NetworkType.SOCKET;
                host = "127.0.0.1";
                port = 1100;
                System.out.println("Avvio in modalità Sandbox (parametri di default).");
            }

            GuiController guiController = new GuiController(primaryStage, null);
            GuiView view = new GuiView(guiController, lobbyModel);
            ServerProxy proxy = ServerProxyFactory.create(networkType, host, port, lobbyModel, view);
            guiController.setServerProxy(proxy);
            System.out.println("GUI: Connecting via " + networkType + " to " + host + ":" + port + " ...");
            proxy.connect();

            // =========================================================
            // GESTIONE GAME SCENE (PRE-CARICAMENTO)
            // =========================================================
            // Se in futuro noterai che il passaggio alla schermata di gioco
            // è lento per colpa delle tante immagini delle carte, puoi "pre-costruire"
            // la GameScene qui in background, mentre l'utente fa il login.
            //
            // GameScene gameScene = new GameScene();
            // guiController.setGameScene(gameScene);
            // =========================================================

            // 7. Apriamo il sipario: fa partire la LoginScene
            guiController.start();

        } catch (Exception e) {
            System.err.println("Errore critico durante l'avvio della GUI:");
            e.printStackTrace();
        }
    }


      // Modalità Sandbox

    public static void main(String[] args) {
        launch(args);
    }
}