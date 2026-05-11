package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InGameMenuOverlay {

    private Font tribalSmall;
    private Font tribalMedium;
    private StackPane rulesOverlay;
    private ImageView rulesImageView;
    private List<Image> rulesPages = new ArrayList<>();
    private int currentPage = 0;

    public VBox buildNode(GuiController controller, Runnable onClose, StackPane rootStack) {
        try {
            tribalSmall = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/tribal.ttf"), 16);
            tribalMedium = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/tribal.ttf"), 24);

            if (rulesPages.isEmpty()) {
                for (int i = 0; i < 8; i++) {
                    rulesPages.add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/rules/page" + i + ".png"))));
                }
            }
        } catch (Exception ignored) {}

        buildRulesOverlay();

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
        if (tribalMedium != null) title.setFont(tribalMedium);

        Button resumeBtn = createMenuButton("RESUME");
        resumeBtn.setOnAction(e -> onClose.run());

        Button rulesBtn = createMenuButton("GAME RULES");
        rulesBtn.setOnAction(e -> {
            if (!rootStack.getChildren().contains(rulesOverlay)) {
                rootStack.getChildren().add(rulesOverlay);
            }
            showRules();
        });

        Button quitBtn = createMenuButton("QUIT GAME");
        quitBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        if (tribalSmall != null) quitBtn.setFont(tribalSmall);
        quitBtn.setOnAction(e -> {
            onClose.run();
            controller.requestReturnToLobby();
        });

        Button exitBtn = createMenuButton("EXIT TO DESKTOP");
        exitBtn.setOnAction(e -> System.exit(0));

        container.getChildren().addAll(title, resumeBtn, rulesBtn, quitBtn, exitBtn);

        return container;
    }

    private void buildRulesOverlay() {
        rulesOverlay = new StackPane();
        rulesOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        rulesOverlay.setVisible(false);
        rulesOverlay.setPadding(new Insets(20));

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);

        rulesImageView = new ImageView();
        rulesImageView.setPreserveRatio(true);
        rulesImageView.setFitHeight(700);
        if (!rulesPages.isEmpty()) rulesImageView.setImage(rulesPages.get(0));

        HBox navBar = new HBox(20);
        navBar.setAlignment(Pos.CENTER);

        Button prevBtn = createMenuButton("PREV");
        prevBtn.setPrefWidth(120);
        prevBtn.setOnAction(e -> navigateRules(-1));

        Button nextBtn = createMenuButton("NEXT");
        nextBtn.setPrefWidth(120);
        nextBtn.setOnAction(e -> navigateRules(1));

        Button closeBtn = createMenuButton("CLOSE");
        closeBtn.setPrefWidth(120);
        closeBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
        closeBtn.setOnAction(e -> rulesOverlay.setVisible(false));

        navBar.getChildren().addAll(prevBtn, closeBtn, nextBtn);
        content.getChildren().addAll(rulesImageView, navBar);
        rulesOverlay.getChildren().add(content);
    }

    private void showRules() {
        currentPage = 0;
        updateRulesDisplay();
        rulesOverlay.setOpacity(0);
        rulesOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), rulesOverlay);
        ft.setToValue(1);
        ft.play();
    }

    private void navigateRules(int dir) {
        int next = currentPage + dir;
        if (next >= 0 && next < rulesPages.size()) {
            currentPage = next;
            updateRulesDisplay();
        }
    }

    private void updateRulesDisplay() {
        if (!rulesPages.isEmpty()) {
            rulesImageView.setImage(rulesPages.get(currentPage));
        }
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setStyle(
                "-fx-background-color: #3e2a1d; " +
                        "-fx-text-fill: #F2D5A3; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        );
        if (tribalSmall != null) btn.setFont(tribalSmall);
        return btn;
    }
}