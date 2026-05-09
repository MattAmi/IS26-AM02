package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.gui.scenes.NetworkPopup;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for the Graphical User Interface.
 * Handles the initial bootstrap of the MVC components and the network proxy.
 */
public class MainGUI extends Application {

    // These static fields are initialized by ClientApp or the popup before/during start
    public static LobbyModel lobbyModel;
    public static NetworkPopup.ConnectionConfig connectionConfig;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. If no configuration is provided, show the network selection popup
            if (connectionConfig == null) {
                connectionConfig = NetworkPopup.displayAndChoose();
            }

            // 2. Initialize the View (GuiView)
            // We pass null as the controller initially to resolve the circular dependency
            GuiView view = new GuiView(null, lobbyModel);

            // 3. Create the Network Proxy using the selected configuration
            ServerProxy proxy = ServerProxyFactory.create(
                    connectionConfig.type(),
                    connectionConfig.host(),
                    connectionConfig.port(),
                    lobbyModel,
                    view
            );

            // 4. Create the GuiController
            // It requires the Stage for scene switching and the Proxy for sending commands
            GuiController guiController = new GuiController(primaryStage, proxy, lobbyModel, view);

            // 5. Finalize the wiring (circular references)
            view.setGuiController(guiController);
            proxy.setClientController(guiController);

            System.out.println("[GUI] Bootstrap completed. Attempting to connect via " +
                    connectionConfig.type() + " to " + connectionConfig.host() + ":" + connectionConfig.port());

            // 6. Launch the first UI scene (usually the login or intro scene)
            guiController.start();

            // 7. Perform the network connection in a background thread
            // This prevents the JavaFX Application Thread from hanging during the handshake
            Thread connectionThread = new Thread(() -> {
                try {
                    proxy.connect();
                    System.out.println("[GUI] Network connection established successfully.");
                } catch (Exception e) {
                    System.err.println("[GUI] Initial connection attempt failed: " + e.getMessage());
                    // Trigger the interactive retry loop if the server is unreachable
                    guiController.promptConnectionAndRetry();
                }
            });

            connectionThread.setDaemon(true); // Ensures the thread terminates when the GUI is closed
            connectionThread.start();

        } catch (Exception e) {
            System.err.println("[FATAL] Critical error during GUI startup sequence:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Standard JavaFX main method, used as a fallback.
     */
    public static void main(String[] args) {
        launch(args);
    }
}