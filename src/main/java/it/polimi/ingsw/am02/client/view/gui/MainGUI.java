package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI extends Application {

    public static LobbyModel lobbyModel;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Creiamo la View (inizialmente senza controller)
            GuiView view = new GuiView(null, lobbyModel);

            // 2. Creiamo il Controller (Il Proxy verrà creato dopo, quando l'utente sceglie la rete)
            GuiController guiController = new GuiController(primaryStage, null, lobbyModel, view);

            // 3. Leghiamo la View al Controller
            view.setGuiController(guiController);

            // 4. Avviamo la SPA!
            guiController.start();

        } catch (Exception e) {
            System.err.println("[FATAL] Critical error during GUI startup:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}