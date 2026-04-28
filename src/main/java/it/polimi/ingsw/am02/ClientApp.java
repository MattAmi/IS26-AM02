package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy;
import it.polimi.ingsw.am02.client.network.socket.SocketServerProxy;
import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.view.AbstractClientView; // o ClientView se è un'interfaccia
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.client.view.gui.GuiView;

import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            // Select IP
            System.out.print("Insert server's IP [press Enter for 127.0.0.1]: ");
            String ip = scanner.nextLine().trim();
            if (ip.isEmpty()) {
                ip = "127.0.0.1";
            }

            // Select between RMI and Socket
            System.out.println("\nPick a network technology:");
            System.out.println("1) RMI");
            System.out.println("2) Socket");
            System.out.print("Picked: ");
            int networkChoice = Integer.parseInt(scanner.nextLine().trim());

            // Select user interface
            System.out.println("\nPick a user interface:");
            System.out.println("1) TUI (Textual)");
            System.out.println("2) GUI (Graphic)");
            System.out.print("Picked: ");
            int viewChoice = Integer.parseInt(scanner.nextLine().trim());

            GameModel model = new GameModel();

            ServerProxy proxy;
            if (networkChoice == 1) {
                proxy = new RmiServerProxy(ip, 1099, model);
            } else {
                proxy = new SocketServerProxy(ip, 1234, model);
            }

            AbstractClientView view;
            if (viewChoice == 1) {
                view = new TuiView(model);
            } else {
                view = new GuiView(model);
            }

            ClientController controller = new ClientController(proxy, model, view);

            proxy.connect();
            view.render();
            controller.run();

        } catch (NumberFormatException e) {
            System.err.println("Insertion Error. Restart and insert a valid number.");
        } catch (Exception e) {
            System.err.println("Fatal error during start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}