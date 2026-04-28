package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;

import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        Scanner setupScanner = new Scanner(System.in);
        try {
            LobbyModel lobbyModel = new LobbyModel();
            TuiView view = new TuiView(lobbyModel);

            System.out.println("Select Network: 1. RMI | 2. Socket");
            String netChoice = setupScanner.nextLine().trim();
            NetworkType networkType = netChoice.equals("2") ? NetworkType.SOCKET : NetworkType.RMI;

            System.out.print("Server IP [127.0.0.1]: ");
            String host = setupScanner.nextLine().trim();
            if (host.isEmpty()) host = "127.0.0.1";

            ServerProxy proxy = ServerProxyFactory.create(networkType, host, 1099, lobbyModel, view);
            ClientController controller = new ClientController(proxy, lobbyModel, view);

            System.out.println("Connecting...");
            proxy.connect();
            controller.run();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}