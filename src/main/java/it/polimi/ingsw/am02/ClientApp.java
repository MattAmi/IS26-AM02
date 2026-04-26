package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.model.ClientModel;
import it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy;
import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.view.tui.TuiView;

public class ClientApp {
    public static void main(String[] args) {
        try {
            ClientModel model = new ClientModel();

            RmiServerProxy proxy = new RmiServerProxy("127.0.0.1", 1099, model);

            TuiView view = new TuiView(model);

            ClientController controller = new ClientController(proxy, model, view);

            proxy.connect();

            view.render();

            controller.run();

        } catch (Exception e) {
            System.err.println("Fatal error during startup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}