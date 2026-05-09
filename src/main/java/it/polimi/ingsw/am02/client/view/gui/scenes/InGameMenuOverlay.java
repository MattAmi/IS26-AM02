package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class InGameMenuOverlay {

    public VBox buildNode(GuiController controller, Runnable onClose) {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40));
        container.setStyle(
                "-fx-background-color: #1C1C1C;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #F2D5A3;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 14;"
        );
        container.setMaxSize(300, 400);

        Label title = new Label("GAME MENU");
        title.setTextFill(Color.web("#F2D5A3"));
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Button resumeBtn = createMenuButton("RESUME");
        resumeBtn.setOnAction(e -> onClose.run());

        Button rulesBtn = createMenuButton("GAME RULES");
        rulesBtn.setOnAction(e -> controller.handleError("Game Rules PDF integration coming soon!"));

        Button quitBtn = createMenuButton("QUIT GAME");
        quitBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        quitBtn.setOnAction(e -> {
            onClose.run();
            controller.requestReturnToLobby();
        });

        Button exitBtn = createMenuButton("EXIT TO DESKTOP");
        exitBtn.setOnAction(e -> System.exit(0));

        container.getChildren().addAll(title, resumeBtn, rulesBtn, quitBtn, exitBtn);

        return container;
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setStyle(
                "-fx-background-color: #3e2a1d; " +
                "-fx-text-fill: #F2D5A3; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand;"
        );
        return btn;
    }
}
