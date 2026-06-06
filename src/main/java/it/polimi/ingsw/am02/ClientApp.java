package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.client.view.gui.GuiView;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import it.polimi.ingsw.am02.client.view.gui.SceneRouter;
import it.polimi.ingsw.am02.client.view.tui.TuiController;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Scanner;

/**
 * Main entrance for the client application.
 * This class allows the user to choose between TUI and GUI interfaces
 * and manages the initial setup for both.
 */
public class ClientApp extends Application {

    private static LobbyModel lobbyModel;
    private static String[] savedArgs;

    /**
     * The main method that starts the application.
     * It prompts the user to select the interface type.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        savedArgs = args;
        Scanner setupScanner = new Scanner(System.in);

        try {
            lobbyModel = new LobbyModel();

            // 1. Interface choice (TUI or GUI)
            System.out.println("Select Interface: [1] TUI | [2] GUI");
            String uiChoice = setupScanner.nextLine().trim();
            boolean useGui = uiChoice.equals("2");

            if (useGui) {
                // --- GUI SETUP ---
                Application.launch(ClientApp.class, args);

            } else {
                // --- TUI SETUP ---
                System.out.println("Select Protocol: [1] RMI | [2] Socket");
                String netChoice = setupScanner.nextLine().trim();
                NetworkType networkType = netChoice.equals("2") ? NetworkType.SOCKET : NetworkType.RMI;

                // Dynamic IP request
                System.out.print("Server IP Address [default: 127.0.0.1]: ");
                String host = setupScanner.nextLine().trim();
                if (host.isEmpty()) host = "127.0.0.1";

                // Dynamic Port request with auto-detection of default based on protocol
                int defaultPort = (networkType == NetworkType.SOCKET) ? 1100 : 1099;
                System.out.print("Server Port [default: " + defaultPort + "]: ");
                String portStr = setupScanner.nextLine().trim();
                int port = portStr.isEmpty() ? defaultPort : Integer.parseInt(portStr);

                // Creation of the MVC ecosystem for the TUI
                ClientView view = new TuiView(lobbyModel);
                ServerProxy proxy = ServerProxyFactory.create(networkType, host, port, lobbyModel, view);

                TuiController controller = new TuiController(proxy, lobbyModel, view, networkType, host, port);
                proxy.setClientController(controller);

                System.out.println("\nConnecting to server via " + networkType + " on " + host + ":" + port + "...");
                proxy.connect();
                controller.run(); // Blocking loop to read commands from terminal
            }

        } catch (Exception e) {
            System.err.println("[FATAL] Client failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Start method for the JavaFX GUI.
     * Initializes the GUI views, controller, and router.
     *
     * @param primaryStage the primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            ImageLoader.preloadRulesInBackground();

            if (lobbyModel == null) {
                lobbyModel = new LobbyModel();
            }

            primaryStage.setOnCloseRequest(event -> {
                javafx.application.Platform.exit();
                System.exit(0);
            });

            GuiView guiView = new GuiView(lobbyModel);
            SceneRouter sceneRouter = new SceneRouter(primaryStage);
            GuiController guiController = new GuiController(null, lobbyModel, guiView);

            guiView.setSceneRouter(sceneRouter);
            sceneRouter.setGuiController(guiController);

            sceneRouter.show();
            sceneRouter.promptConnectionAndRetry(lobbyModel, guiView);

        } catch (Exception e) {
            System.err.println("[FATAL] Critical error during GUI startup:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}