package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.gui.MainGUI;
import it.polimi.ingsw.am02.client.view.tui.TuiController;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import javafx.application.Application;

import java.util.Scanner;

public class ClientApp {

    private static final int RMI_PORT    = 1099;
    private static final int SOCKET_PORT = 1100;

    public static void main(String[] args) {
        Scanner setupScanner = new Scanner(System.in);

        try {
            LobbyModel lobbyModel = new LobbyModel();

            // 1. UI Choice
            System.out.println("Select Interface: [1] TUI | [2] GUI");
            String uiChoice = setupScanner.nextLine().trim();
            boolean useGui = uiChoice.equals("2");

            if (useGui) {
                // --- GUI SETUP ---
                MainGUI.lobbyModel = lobbyModel;
                Application.launch(MainGUI.class, args);

            } else {
                // --- TUI SETUP ---
                System.out.println("Select Protocol: [1] RMI | [2] Socket");
                String netChoice = setupScanner.nextLine().trim();
                NetworkType networkType = netChoice.equals("2") ? NetworkType.SOCKET : NetworkType.RMI;
                int port = (networkType == NetworkType.SOCKET) ? SOCKET_PORT : RMI_PORT;

                System.out.print("Server IP Address [default: 127.0.0.1]: ");
                String host = setupScanner.nextLine().trim();
                if (host.isEmpty()) host = "127.0.0.1";

                ClientView view = new TuiView(lobbyModel);
                ServerProxy proxy = ServerProxyFactory.create(networkType, host, port, lobbyModel, view);

                TuiController controller = new TuiController(proxy, lobbyModel, view);
                proxy.setClientController(controller);

                System.out.println("\nConnecting to server via " + networkType + "...");
                proxy.connect();
                controller.run();
            }

        } catch (Exception e) {
            System.err.println("[FATAL] Client failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}