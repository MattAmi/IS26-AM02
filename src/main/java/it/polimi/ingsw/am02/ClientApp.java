package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;

public class ClientApp {
    public static void main(String[] args) {
        try {
            // 1. The client ALWAYS starts in the lobby phase. GameModel doesn't exist yet.
            LobbyModel lobbyModel = new LobbyModel();

            // 2. We instantiate the View with the LobbyModel.
            // (Note: The UI team will need to ensure TuiView can switch to GameModel later)
            TuiView view = new TuiView(lobbyModel);

            // 3. We use the team's Factory to create the proxy.
            // For now it's hardcoded to RMI, later you can add a Scanner to ask the user.
            ServerProxy proxy = ServerProxyFactory.create(
                    NetworkType.RMI,
                    "127.0.0.1",
                    1099,
                    lobbyModel,
                    view
            );

            // 4. Initialize the Controller
            ClientController controller = new ClientController(proxy, lobbyModel, view);

            // 5. Connect and start the input loop
            System.out.println("Connecting to server...");
            proxy.connect();
            controller.run();

        } catch (Exception e) {
            System.err.println("Fatal error during startup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}