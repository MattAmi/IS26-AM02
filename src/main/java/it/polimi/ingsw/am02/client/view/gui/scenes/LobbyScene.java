package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the lobby scene of the game.
 * This scene allows players to set their nickname and select a totem color.
 */
public class LobbyScene {

    private GuiController controller;
    private VBox playerList;
    private VBox nicknameBox;
    private VBox totemBox;
    private TextField nickField;
    private String myNickname;

    private Font tribalSmall;
    private Font tribalMedium;
    private Font tribalLarge;
    private Font introFont;

    private final Totem[] totems = Totem.values();
    private final Map<Totem, ImageView> totemViews = new HashMap<>();
    private final List<Transition> runningTransitions = new ArrayList<>();

    private int currentCarouselIndex = 0;
    private Label selectedTotemLabel;
    private Button confirmTotemBtn;

    private Button prevBtn;
    private Button nextBtn;

    private Map<String, Totem> currentChosenTotems = new HashMap<>();

    /**
     * Builds the lobby scene node.
     *
     * @param controller The GUI controller.
     * @return The constructed Region representing the scene.
     */
    public Region buildNode(GuiController controller) {
        this.controller = controller;

        try {
            tribalSmall = Font.loadFont(getClass().getResourceAsStream("/fonts/tribal.ttf"), 16);
            tribalMedium = Font.loadFont(getClass().getResourceAsStream("/fonts/tribal.ttf"), 24);
            tribalLarge = Font.loadFont(getClass().getResourceAsStream("/fonts/tribal.ttf"), 48);
            introFont = Font.loadFont(getClass().getResourceAsStream("/fonts/intro.ttf"), 18);
        } catch (Exception ignored) {
        }

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
            Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/mesos_lobby.png")));
            backgroundView.setImage(img);
            double iw = img.getWidth();
            double ih = img.getHeight();
            double vw = iw * 0.8;
            double vh = ih * 0.8;
            double vx = (iw - vw) / 2;
            double vy = (ih - vh) / 2;
            backgroundView.setViewport(new Rectangle2D(vx, vy, vw, vh));
        } catch (Exception ignored) {
        }

        backgroundView.fitWidthProperty().bind(bgContainer.widthProperty());
        backgroundView.fitHeightProperty().bind(bgContainer.heightProperty());
        backgroundView.setPreserveRatio(false);
        backgroundView.setEffect(new GaussianBlur(10));

        ScaleTransition stBg = new ScaleTransition(Duration.seconds(20), backgroundView);
        stBg.setFromX(1.0);
        stBg.setFromY(1.0);
        stBg.setToX(1.25);
        stBg.setToY(1.25);
        stBg.setCycleCount(Animation.INDEFINITE);
        stBg.setAutoReverse(true);
        stBg.play();
        bgContainer.getChildren().add(backgroundView);

        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        overlay.setMouseTransparent(true);

        BorderPane uiLayer = new BorderPane();
        uiLayer.setPickOnBounds(false);

        StackPane topBar = new StackPane();
        topBar.setPadding(new Insets(30));
        Label header = new Label("LOBBY PREPARATION");
        header.setTextFill(Color.web("#F2D5A3"));
        if (introFont != null) header.setFont(Font.font(introFont.getFamily(), 48));
        uiLayer.setTop(topBar);

        StackPane centerStack = new StackPane();

        nicknameBox = new VBox(20);
        nicknameBox.setAlignment(Pos.CENTER);
        nicknameBox.setMaxWidth(450);
        nicknameBox.setStyle("-fx-background-color: rgba(43, 29, 20, 0.85); -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 15; -fx-padding: 40;");

        Label nickLabel = new Label("NICKNAME");
        nickLabel.setTextFill(Color.WHITE);
        if (introFont != null) nickLabel.setFont(Font.font(introFont.getFamily(), 24));

        nickField = new TextField();
        nickField.setPromptText("Enter your name...");
        nickField.setMaxWidth(300);
        nickField.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-color: #F2D5A3; -fx-alignment: center;");
        if (introFont != null) nickField.setFont(Font.font(introFont.getFamily(), 16));

        Button confirmNick = new Button("CONFIRM");
        confirmNick.setPrefSize(200, 50);
        confirmNick.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #F2D5A3;");
        if (tribalSmall != null) confirmNick.setFont(tribalSmall);
        javafx.event.EventHandler<javafx.event.ActionEvent> confirmNickAction = e -> {
            String inputText = nickField.getText();
            if (!inputText.isBlank()) {
                if (inputText.equals(myNickname)) {
                    nickField.setEditable(false);
                    nicknameBox.setVisible(false);
                    totemBox.setVisible(true);
                    updateCarouselVisuals();
                } else {
                    nickField.setEditable(false);
                    controller.requestSetUsername(inputText);
                }
            }
        };
        confirmNick.setOnAction(confirmNickAction);
        nickField.setOnAction(confirmNickAction);
        nicknameBox.getChildren().addAll(nickLabel, nickField, confirmNick);

        totemBox = new VBox(20);
        totemBox.setAlignment(Pos.CENTER);
        totemBox.setVisible(false);

        StackPane carouselPane = new StackPane();
        carouselPane.setMinSize(700, 450);

        for (Totem totem : totems) {
            ImageView view = new ImageView();
            String colorName = getTotemColorName(totem);
            try {
                Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/totems/totem_" + colorName + ".png")));
                view.setImage(img);
            } catch (Exception ignored) {
            }
            view.setFitHeight(220);
            view.setPreserveRatio(true);
            totemViews.put(totem, view);
            carouselPane.getChildren().add(view);
        }

        HBox carouselControls = new HBox(10);
        carouselControls.setAlignment(Pos.CENTER);

        prevBtn = new Button("<");
        prevBtn.setStyle("-fx-base: #3e2a1d; -fx-text-fill: #F2D5A3; -fx-cursor: hand; -fx-background-radius: 50;");
        if (tribalLarge != null) prevBtn.setFont(tribalLarge);
        prevBtn.setOnAction(e -> rotateCarousel(-1));

        nextBtn = new Button(">");
        nextBtn.setStyle("-fx-base: #3e2a1d; -fx-text-fill: #F2D5A3; -fx-cursor: hand; -fx-background-radius: 50;");
        if (tribalLarge != null) nextBtn.setFont(tribalLarge);
        nextBtn.setOnAction(e -> rotateCarousel(1));

        carouselControls.getChildren().addAll(prevBtn, carouselPane, nextBtn);

        selectedTotemLabel = new Label("");
        selectedTotemLabel.setTextFill(Color.web("#F2D5A3"));
        if (introFont != null) selectedTotemLabel.setFont(Font.font(introFont.getFamily(), 24));

        confirmTotemBtn = new Button("CONFIRM TOTEM");
        confirmTotemBtn.setPrefSize(250, 50);
        confirmTotemBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #F2D5A3;");
        if (tribalSmall != null) confirmTotemBtn.setFont(tribalSmall);

        confirmTotemBtn.setOnAction(e -> {
            Totem selected = totems[currentCarouselIndex];
            controller.requestSelectTotem(selected);

        });

        Button backBtn = new Button("BACK");
        backBtn.setPrefSize(250, 40);
        backBtn.setStyle("-fx-base: #555555; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #888888;");
        if (tribalSmall != null) backBtn.setFont(tribalSmall);
        backBtn.setOnAction(e -> {
            nickField.setEditable(true);
            totemBox.setVisible(false);
            nicknameBox.setVisible(true);
            updateCarouselVisuals();
        });

        totemBox.getChildren().addAll(carouselControls, selectedTotemLabel, confirmTotemBtn, backBtn);

        centerStack.getChildren().addAll(nicknameBox, totemBox);
        uiLayer.setCenter(centerStack);

        VBox rightBar = new VBox(20);
        rightBar.setPadding(new Insets(30, 20, 30, 20));
        rightBar.setPrefWidth(280);
        rightBar.setAlignment(Pos.TOP_CENTER);
        rightBar.setStyle("-fx-background-color: rgba(43, 29, 20, 0.7); -fx-border-color: #F2D5A3; -fx-border-width: 0 0 0 2;");

        Label pHeader = new Label("PLAYERS");
        pHeader.setTextFill(Color.web("#F2D5A3"));
        if (introFont != null) pHeader.setFont(Font.font(introFont.getFamily(), 24));

        playerList = new VBox(15);
        playerList.setAlignment(Pos.TOP_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button leaveBtn = new Button("LEAVE LOBBY");
        leaveBtn.setMaxWidth(Double.MAX_VALUE);
        leaveBtn.setStyle("-fx-base: #8B0000; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: white;");
        if (tribalSmall != null) leaveBtn.setFont(tribalSmall);
        leaveBtn.setOnAction(e -> controller.requestLeaveLobby());

        rightBar.getChildren().addAll(pHeader, new Separator(), playerList, spacer, leaveBtn);
        uiLayer.setRight(rightBar);

        root.getChildren().addAll(bgContainer, overlay, uiLayer);
        return root;
    }

    /**
     * Returns the name of the totem color for resource path construction.
     *
     * @param t The Totem enum value.
     * @return The lowercase color name.
     */
    private String getTotemColorName(Totem t) {
        return t.name().toLowerCase();
    }

    /**
     * Rotates the totem carousel.
     *
     * @param dir The direction to rotate (-1 for left, 1 for right).
     */
    private void rotateCarousel(int dir) {
        currentCarouselIndex = (currentCarouselIndex + dir + totems.length) % totems.length;
        updateCarouselVisuals();
    }

    /**
     * Updates the visual representation of the totem carousel based on the current selection.
     */
    private void updateCarouselVisuals() {
        for (Transition transition : runningTransitions) {
            transition.stop();
        }
        runningTransitions.clear();

        double rX = 260;
        double rY = 100;

        for (int i = 0; i < totems.length; i++) {
            final Totem t = totems[i];
            final ImageView view = totemViews.get(t);

            int offset = (i - currentCarouselIndex + totems.length) % totems.length;
            double angle = offset * (2.0 * Math.PI / totems.length);

            double tx = rX * Math.sin(angle);
            double ty = rY * Math.cos(angle);

            double depthFactor = (ty + rY) / (2.0 * rY);
            double targetScale = 0.55 + (0.45 * depthFactor);
            double opacity = 0.35 + (0.65 * depthFactor);

            String owner = null;
            for (Map.Entry<String, Totem> entry : currentChosenTotems.entrySet()) {
                if (entry.getValue() == t) {
                    owner = entry.getKey();
                    break;
                }
            }

            if (owner != null && !owner.equals(myNickname)) {
                ColorAdjust desaturate = new ColorAdjust();
                desaturate.setSaturation(-1.0);
                desaturate.setBrightness(-0.4);
                view.setEffect(desaturate);
                opacity *= 0.4;
            } else {
                view.setEffect(null);
            }

            TranslateTransition tt = new TranslateTransition(Duration.millis(350), view);
            tt.setToX(tx);
            tt.setToY(ty);
            runningTransitions.add(tt);
            tt.play();

            ScaleTransition st = new ScaleTransition(Duration.millis(350), view);
            st.setToX(targetScale);
            st.setToY(targetScale);
            runningTransitions.add(st);
            st.play();

            FadeTransition ft = new FadeTransition(Duration.millis(350), view);
            ft.setToValue(opacity);
            runningTransitions.add(ft);
            ft.play();

            if (offset == 0) {
                view.toFront();
                selectedTotemLabel.setText(t.name().toUpperCase());

                final int capturedIndex = currentCarouselIndex;
                st.setOnFinished(e -> {
                    if (currentCarouselIndex == capturedIndex) {
                        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), view);
                        pulse.setFromX(targetScale);
                        pulse.setFromY(targetScale);
                        pulse.setToX(targetScale * 1.15);
                        pulse.setToY(targetScale * 1.15);
                        pulse.setAutoReverse(true);
                        pulse.setCycleCount(Animation.INDEFINITE);
                        runningTransitions.add(pulse);
                        pulse.play();
                    }
                });
            } else {
                if (ty < 0) view.toBack();
            }
        }
        updateConfirmButton();
    }

    /**
     * Updates the state of the totem confirmation button.
     */
    private void updateConfirmButton() {
        Totem selected = totems[currentCarouselIndex];
        String owner = null;
        for (Map.Entry<String, Totem> entry : currentChosenTotems.entrySet()) {
            if (entry.getValue() == selected) {
                owner = entry.getKey();
                break;
            }
        }

        if (owner != null && !owner.equals(myNickname)) {
            confirmTotemBtn.setDisable(true);
            confirmTotemBtn.setText("TAKEN BY " + owner);
            confirmTotemBtn.setStyle("-fx-base: #444; -fx-text-fill: #888;");
        } else if (owner != null && owner.equals(myNickname)) {
            confirmTotemBtn.setDisable(true);
            confirmTotemBtn.setText("THIS IS YOUR TOTEM");
            confirmTotemBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: #F2D5A3; -fx-border-color: #F2D5A3;");
        } else {
            confirmTotemBtn.setDisable(false);
            confirmTotemBtn.setText("CONFIRM TOTEM");
            confirmTotemBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #F2D5A3;");
        }
    }

    /**
     * Handles the acceptance of a nickname.
     *
     * @param nickname The accepted nickname.
     */
    public void onNicknameAccepted(String nickname) {
        this.myNickname = nickname;
        Platform.runLater(() -> {
            nicknameBox.setVisible(false);
            totemBox.setVisible(true);
            updateCarouselVisuals();
        });
    }

    /**
     * Handles the rejection of a nickname.
     */
    public void onNicknameRejected() {
        Platform.runLater(() -> {
            nickField.setEditable(true);
            nickField.setText("");
        });
    }

    /**
     * Updates the lobby state UI with player list and chosen totems.
     *
     * @param lobby The current lobby information.
     */
    public void updateLobbyState(LobbyInfo lobby) {
        Platform.runLater(() -> {
            currentChosenTotems = lobby.chosenTotems();
            playerList.getChildren().clear();

            for (String nick : lobby.currentPlayers()) {
                Totem t = currentChosenTotems.get(nick);
                String colorSuffix = (t != null) ? " [" + t.name().toUpperCase() + "]" : " (picking...)";
                Label l = new Label("• " + nick + colorSuffix);
                if (introFont != null) l.setFont(introFont);
                l.setTextFill(nick.equals(myNickname) ? Color.LIME : Color.WHITE);
                playerList.getChildren().add(l);
            }
            if (totemBox.isVisible()) updateCarouselVisuals();
        });
    }
}