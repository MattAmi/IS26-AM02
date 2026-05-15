package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainGUI extends Application {

    public static LobbyModel lobbyModel;

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

    public static void main(String[] args) {
        launch(args);
    }
}