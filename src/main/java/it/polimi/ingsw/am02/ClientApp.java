package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;

import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        // Using try-with-resources to ensure the setup Scanner is properly closed
        // (though System.in remains open for the Controller)
        Scanner setupScanner = new Scanner(System.in);

        try {
            System.out.println("========================================");
            System.out.println("          MESOS - CLIENT SETUP          ");
            System.out.println("========================================");

            // 1. Choose User Interface
            System.out.println("Select Interface:");
            System.out.println("1. TUI (Text User Interface)");
            System.out.println("2. GUI (Graphical User Interface) - WIP");
            System.out.print("> ");
            String uiChoice = setupScanner.nextLine().trim();
            boolean useGui = uiChoice.equals("2");

            // 2. Choose Network Type
            System.out.println("\nSelect Network Connection:");
            System.out.println("1. RMI");
            System.out.println("2. Socket");
            System.out.print("> ");
            String netChoice = setupScanner.nextLine().trim();
            NetworkType networkType = netChoice.equals("2") ? NetworkType.SOCKET : NetworkType.RMI;

            // 3. Enter Server Details
            System.out.print("\nServer IP [default: 127.0.0.1]: ");
            String host = setupScanner.nextLine().trim();
            if (host.isEmpty()) host = "127.0.0.1";

            // Default ports: 1099 for RMI, 1234 for Socket (adjust if your team chose differently)
            int defaultPort = (networkType == NetworkType.RMI) ? 1099 : 1234;
            System.out.print("Server Port [default: " + defaultPort + "]: ");
            String portStr = setupScanner.nextLine().trim();
            int port = portStr.isEmpty() ? defaultPort : Integer.parseInt(portStr);

            System.out.println("\nInitializing client architecture...");

            // --- ARCHITECTURE INITIALIZATION ---

            // The client ALWAYS starts in the lobby phase.
            LobbyModel lobbyModel = new LobbyModel();

            ClientView view;
            if (useGui) {
                System.out.println("[!] GUI is not implemented yet. Falling back to TUI.");
                view = new TuiView(lobbyModel);
            } else {
                view = new TuiView(lobbyModel);
            }

            // We use the team's Factory to create the specific network proxy
            ServerProxy proxy = ServerProxyFactory.create(
                    networkType,
                    host,
                    port,
                    lobbyModel,
                    view
            );

            // Initialize the Controller
            ClientController controller = new ClientController(proxy, lobbyModel, view);

            // Connect to the server
            System.out.println("Connecting to server at " + host + ":" + port + " via " + networkType + "...");
            proxy.connect();

            // Clear the screen slightly before jumping into the game loop
            System.out.println("\nConnection established successfully!");

            // Handoff the control to the blocking input loop of the Controller
            controller.run();

        } catch (Exception e) {
            System.err.println("\n[FATAL ERROR] Cannot start the client: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}