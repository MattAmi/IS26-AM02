package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.gui.scenes.NetworkPopup;
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

            if (networkType == null) {
                networkType = NetworkPopup.displayAndChoose();

                host = "127.0.0.1"; // Oppure puoi inserire un TextField nel popup per chiederlo!
                port = (networkType == NetworkType.SOCKET) ? 1100 : 1099;
            }
            // 1. Create the View first (with null controller for now)
            GuiView view = new GuiView(null, lobbyModel);
            
            // 2. Create the Controller with the actual View
            GuiController guiController = new GuiController(primaryStage, null, lobbyModel, view);
            
            // 3. Link the controller back to the view
            view.setGuiController(guiController);
            
            // 4. Create and link the Proxy
            ServerProxy proxy = ServerProxyFactory.create(networkType, host, port, lobbyModel, view);
            guiController.setServerProxy(proxy);
            proxy.setClientController(guiController);
            
            System.out.println("GUI: Bootstrap completed. Connecting...");
            proxy.connect();

            guiController.start();

        } catch (Exception e) {
            System.err.println("Critical error during GUI startup:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
