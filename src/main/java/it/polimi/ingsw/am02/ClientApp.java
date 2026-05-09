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

    public static void main(String[] args) {
        Scanner setupScanner = new Scanner(System.in);

        try {
            LobbyModel lobbyModel = new LobbyModel();

            // 1. Scelta dell'interfaccia (TUI o GUI)
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

                // Richiesta IP dinamica
                System.out.print("Server IP Address [default: 127.0.0.1]: ");
                String host = setupScanner.nextLine().trim();
                if (host.isEmpty()) host = "127.0.0.1";

                // Richiesta Porta dinamica con auto-rilevamento del default in base al protocollo
                int defaultPort = (networkType == NetworkType.SOCKET) ? 1100 : 1099;
                System.out.print("Server Port [default: " + defaultPort + "]: ");
                String portStr = setupScanner.nextLine().trim();
                int port = portStr.isEmpty() ? defaultPort : Integer.parseInt(portStr);

                // Creazione dell'ecosistema MVC per la TUI
                ClientView view = new TuiView(lobbyModel);
                ServerProxy proxy = ServerProxyFactory.create(networkType, host, port, lobbyModel, view);

                TuiController controller = new TuiController(proxy, lobbyModel, view);
                proxy.setClientController(controller);

                System.out.println("\nConnecting to server via " + networkType + " on " + host + ":" + port + "...");
                proxy.connect();
                controller.run(); // Loop bloccante per leggere i comandi da terminale
            }

        } catch (Exception e) {
            System.err.println("[FATAL] Client failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}