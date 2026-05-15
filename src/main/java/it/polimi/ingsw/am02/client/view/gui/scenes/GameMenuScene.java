package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class GameMenuScene {

    private GuiController controller;
    private StackPane menuContainer;
    private VBox mainButtonsBox;
    private VBox createLobbyBox;
    private VBox reconnectBox;
    private StackPane rulesOverlay;
    private ImageView rulesImageView;
    private List<Image> rulesPages = new ArrayList<>();
    private int currentPage = 0;
    private Font tribalSmall;
    private Font tribalLarge;
    private Font introFont;

    public Region buildNode(GuiController controller, Runnable onShowLobbyList, Consumer<String> onError) {
        this.controller = controller;

        try {
            tribalSmall = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/tribal.ttf"), 16);
            tribalLarge = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/tribal.ttf"), 100);
            introFont = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 18);

            for (int i = 0; i < 8; i++) {
                rulesPages.add(ImageLoader.getImage("/it.polimi.ingsw.am02.images/rules/page" + i + ".png"));
            }
        } catch (Exception ignored) {}

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        Pane bgContainer = new Pane();
        bgContainer.setMouseTransparent(true);
        bgContainer.prefWidthProperty().bind(root.widthProperty());
        bgContainer.prefHeightProperty().bind(root.heightProperty());

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        bgContainer.setClip(clip);

        ImageView backgroundView = new ImageView();
        try {
            Image img = ImageLoader.getImage("/it.polimi.ingsw.am02.images/mesos_box.png");            backgroundView.setImage(img);
            double iw = img.getWidth();
            double ih = img.getHeight();
            double vw = iw * 0.7;
            double vh = ih * 0.7;
            double vx = (iw - vw) / 2;
            double vy = (ih - vh) / 2;
            backgroundView.setViewport(new Rectangle2D(vx, vy, vw, vh));
        } catch (Exception ignored) {}

        backgroundView.fitWidthProperty().bind(bgContainer.widthProperty());
        backgroundView.fitHeightProperty().bind(bgContainer.heightProperty());
        backgroundView.setPreserveRatio(false);
        backgroundView.setEffect(new GaussianBlur(10));

        ScaleTransition st = new ScaleTransition(Duration.seconds(20), backgroundView);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.25); st.setToY(1.25);
        st.setCycleCount(ScaleTransition.INDEFINITE); st.setAutoReverse(true); st.play();

        bgContainer.getChildren().add(backgroundView);

        VBox uiLayer = new VBox(30);
        uiLayer.setAlignment(Pos.CENTER);
        uiLayer.setPickOnBounds(false);

        Label title = new Label("MESOS");
        title.setTextFill(Color.web("#F2D5A3"));
        if (tribalLarge != null) title.setFont(tribalLarge);

        menuContainer = new StackPane();
        menuContainer.setMaxSize(450, 550);

        buildMainButtons(onShowLobbyList);
        buildCreateLobbyForm(onError);
        buildReconnectForm(onError);
        buildRulesOverlay();

        menuContainer.getChildren().addAll(createLobbyBox, reconnectBox, mainButtonsBox);
        createLobbyBox.setVisible(false);
        reconnectBox.setVisible(false);

        uiLayer.getChildren().addAll(title, menuContainer);
        root.getChildren().addAll(bgContainer, uiLayer, rulesOverlay);

        return root;
    }

    private void buildMainButtons(Runnable onShowLobbyList) {
        mainButtonsBox = new VBox(20);
        mainButtonsBox.setAlignment(Pos.CENTER);
        mainButtonsBox.getChildren().addAll(
                createMenuButton("CREATE LOBBY", e -> switchInternalMenu(createLobbyBox)),
                createMenuButton("JOIN LOBBY", e -> onShowLobbyList.run()),
                createMenuButton("RECONNECT", e -> switchInternalMenu(reconnectBox)),
                createMenuButton("GAME RULES", e -> showRules()),
                createQuitButton()
        );
    }

    private void buildRulesOverlay() {
        rulesOverlay = new StackPane();
        rulesOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
        rulesOverlay.setVisible(false);
        rulesOverlay.setPadding(new Insets(40));

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(1200);

        rulesImageView = new ImageView();
        rulesImageView.setPreserveRatio(true);
        rulesImageView.setFitHeight(850);
        if (!rulesPages.isEmpty()) rulesImageView.setImage(rulesPages.get(0));

        HBox navigationBar = new HBox();
        navigationBar.setAlignment(Pos.CENTER);
        navigationBar.setSpacing(10);

        Button prevBtn = createMenuButton("PREV", e -> navigateRules(-1));
        prevBtn.setPrefSize(150, 45);

        Button nextBtn = createMenuButton("NEXT", e -> navigateRules(1));
        nextBtn.setPrefSize(150, 45);

        Button closeBtn = createMenuButton("CLOSE", e -> rulesOverlay.setVisible(false));
        closeBtn.setPrefSize(150, 45);
        closeBtn.setStyle("-fx-base: #8B0000; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: white; -fx-border-radius: 5;");

        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);

        navigationBar.getChildren().addAll(prevBtn, spacer1, closeBtn, spacer2, nextBtn);
        content.getChildren().addAll(rulesImageView, navigationBar);
        rulesOverlay.getChildren().add(content);
    }

    private void showRules() {
        currentPage = 0; updateRulesDisplay();
        rulesOverlay.setOpacity(0); rulesOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), rulesOverlay);
        ft.setToValue(1); ft.play();
    }

    private void navigateRules(int direction) {
        int next = currentPage + direction;
        if (next >= 0 && next < rulesPages.size()) {
            currentPage = next; updateRulesDisplay();
        }
    }

    private void updateRulesDisplay() {
        if (!rulesPages.isEmpty()) rulesImageView.setImage(rulesPages.get(currentPage));
    }

    private void buildCreateLobbyForm(Consumer<String> onError) {
        createLobbyBox = new VBox(15);
        createLobbyBox.setAlignment(Pos.CENTER);
        createLobbyBox.setStyle("-fx-background-color: rgba(43, 29, 20, 0.95); -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 15; -fx-padding: 30;");

        Label lbl = new Label("Select Lobby Size (2-5):");
        lbl.setTextFill(Color.WHITE);
        if (tribalSmall != null) lbl.setFont(tribalSmall);

        TextField sizeField = new TextField("2");
        sizeField.setMaxWidth(100); sizeField.setAlignment(Pos.CENTER);
        sizeField.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-color: #F2D5A3;");
        if (tribalSmall != null) sizeField.setFont(tribalSmall);

        createLobbyBox.getChildren().addAll(lbl, sizeField,
                createMenuButton("CONFIRM", e -> {
                    try {
                        int size = Integer.parseInt(sizeField.getText());
                        if (size >= 2 && size <= 5) controller.requestCreateLobby(size);
                        else onError.accept("Size must be between 2 and 5.");
                    } catch (Exception ex) { onError.accept("Invalid format."); }
                }),
                createMenuButton("BACK", e -> switchInternalMenu(mainButtonsBox))
        );
    }

    private void buildReconnectForm(Consumer<String> onError) {
        reconnectBox = new VBox(15);
        reconnectBox.setAlignment(Pos.CENTER);
        reconnectBox.setStyle("-fx-background-color: rgba(43, 29, 20, 0.95); -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 15; -fx-padding: 30;");

        Label lblNick = new Label("Nickname:");
        lblNick.setTextFill(Color.WHITE);
        if (introFont != null) lblNick.setFont(introFont);

        TextField nickField = new TextField();
        nickField.setPromptText("Enter Nickname"); nickField.setMaxWidth(220);
        nickField.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-color: #F2D5A3;");
        if (introFont != null) nickField.setFont(introFont);

        Label lblId = new Label("Game ID:");
        lblId.setTextFill(Color.WHITE);
        if (introFont != null) lblId.setFont(introFont);

        TextField gameIdField = new TextField();
        gameIdField.setPromptText("Enter Game ID"); gameIdField.setMaxWidth(220);
        gameIdField.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-color: #F2D5A3;");
        if (introFont != null) gameIdField.setFont(introFont);

        reconnectBox.getChildren().addAll(lblNick, nickField, lblId, gameIdField,
                createMenuButton("RECONNECT", e -> {
                    if (!nickField.getText().isBlank() && !gameIdField.getText().isBlank())
                        controller.requestReconnect(nickField.getText(), gameIdField.getText());
                    else onError.accept("Fill both fields.");
                }),
                createMenuButton("BACK", e -> switchInternalMenu(mainButtonsBox))
        );
    }

    private Button createMenuButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
        Button btn = new Button(text);
        btn.setPrefSize(280, 55);
        btn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #F2D5A3; -fx-border-width: 1; -fx-border-radius: 5;");
        if (tribalSmall != null) btn.setFont(tribalSmall);
        btn.setOnAction(event);
        return btn;
    }

    private Button createQuitButton() {
        Button quitBtn = new Button("QUIT GAME");
        quitBtn.setPrefSize(280, 55);
        quitBtn.setStyle("-fx-base: #8B0000; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: white; -fx-border-width: 1; -fx-border-radius: 5;");
        if (tribalSmall != null) quitBtn.setFont(tribalSmall);
        quitBtn.setOnAction(e -> System.exit(0));
        return quitBtn;
    }

    private void switchInternalMenu(VBox menuToShow) {
        mainButtonsBox.setVisible(false); createLobbyBox.setVisible(false);
        reconnectBox.setVisible(false); menuToShow.setVisible(true);
    }
}