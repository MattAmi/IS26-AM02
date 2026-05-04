package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.view.gui.GuiController;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class LobbyScene {

    private GuiController controller;
    private StackPane root;
    private VBox nicknamePhaseBox;
    private VBox totemPhaseBox;
    private TextField nameInput;
    private Label currentColorLabel;

    private int currentTotemIndex = 0;
    private final List<Image> totemImages = new ArrayList<>();
    private final List<String> totemColors = new ArrayList<>();

    private final ImageView[] totemNodes = new ImageView[5];
    private final int[] nodeSlots = {0, 1, 2, 3, 4};
    private final CarouselPosition[] slots = new CarouselPosition[5];

    private String customFontFamily = "System";
    private Button leftBtn;
    private Button rightBtn;
    private ScaleTransition pulseTransition;

    public Scene buildScene(GuiController controller) {
        this.controller = controller;
        root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        ImageView bgImageView = new ImageView();
        Image bgImage = new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/mesos_lobby.png"));
        bgImageView.setImage(bgImage);
        bgImageView.setPreserveRatio(true);

        bgImageView.fitWidthProperty().bind(root.widthProperty().add(100));

        TranslateTransition move = new TranslateTransition(Duration.seconds(15), bgImageView);
        move.setFromX(-20);
        move.setToX(20);
        move.setFromY(-30);
        move.setToY(30);
        move.setAutoReverse(true);
        move.setCycleCount(TranslateTransition.INDEFINITE);
        move.play();

        ScaleTransition zoom = new ScaleTransition(Duration.seconds(20), bgImageView);
        zoom.setFromX(1.0);
        zoom.setFromY(1.0);
        zoom.setToX(1.05);
        zoom.setToY(1.05);
        zoom.setAutoReverse(true);
        zoom.setCycleCount(ScaleTransition.INDEFINITE);
        zoom.play();

        String fontUrl = getClass().getResource("/it.polimi.ingsw.am02.fonts/tribal.ttf").toExternalForm();
        Font baseFont = Font.loadFont(fontUrl, 10);
        if (baseFont != null) {
            customFontFamily = baseFont.getFamily();
        }

        loadTotemAssets();
        setupCarouselSlots();
        createNicknamePhase();
        createTotemPhase();
        AnchorPane bottomControls = createBottomControls();

        totemPhaseBox.setVisible(false);

        root.getChildren().addAll(bgImageView, nicknamePhaseBox, totemPhaseBox, bottomControls);

        return new Scene(root, 1280, 720);
    }

    private void loadTotemAssets() {
        totemImages.add(new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/totem_orange.png")));
        totemImages.add(new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/totem_yellow.png")));
        totemImages.add(new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/totem_blue.png")));
        totemImages.add(new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/totem_purple.png")));
        totemImages.add(new Image(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/totem_white.png")));

        totemColors.add("ORANGE");
        totemColors.add("YELLOW");
        totemColors.add("TEAL");
        totemColors.add("PURPLE");
        totemColors.add("WHITE");
    }

    private void setupCarouselSlots() {
        slots[0] = new CarouselPosition(0, 0, 1.0);
        slots[1] = new CarouselPosition(250, -40, 0.7);
        slots[2] = new CarouselPosition(120, -90, 0.45);
        slots[3] = new CarouselPosition(-120, -90, 0.45);
        slots[4] = new CarouselPosition(-250, -40, 0.7);
    }

    private void createNicknamePhase() {
        nicknamePhaseBox = new VBox(20);
        nicknamePhaseBox.setAlignment(Pos.CENTER);

        Label title = new Label("CHOOSE YOUR NICKNAME");
        title.setFont(Font.font(customFontFamily, 40));
        title.setTextFill(Color.web("#F2D5A3"));

        nameInput = new TextField();
        nameInput.setMaxWidth(400);
        nameInput.setPromptText("Enter nickname...");
        nameInput.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 24px; -fx-background-radius: 10px;");

        Button submitBtn = new Button("CONFIRM");
        submitBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 24px; -fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
        submitBtn.setPrefSize(200, 50);

        submitBtn.disableProperty().bind(nameInput.textProperty().isEmpty());

        submitBtn.setOnAction(e -> {
            switchToTotemPhase();
        });

        nicknamePhaseBox.getChildren().addAll(title, nameInput, submitBtn);
    }

    private void createTotemPhase() {
        totemPhaseBox = new VBox(30);
        totemPhaseBox.setAlignment(Pos.CENTER);

        Label title = new Label("CHOOSE YOUR TRIBE TOTEM");
        title.setFont(Font.font(customFontFamily, 40));
        title.setTextFill(Color.web("#F2D5A3"));

        currentColorLabel = new Label(totemColors.get(0));
        currentColorLabel.setFont(Font.font(customFontFamily, 24));
        currentColorLabel.setTextFill(Color.web("#A9A9A9"));

        StackPane carouselContainer = new StackPane();
        carouselContainer.setMinHeight(450);

        for (int i = 0; i < 5; i++) {
            totemNodes[i] = new ImageView(totemImages.get(i));
            totemNodes[i].setFitWidth(180);
            totemNodes[i].setFitHeight(360);

            CarouselPosition pos = slots[nodeSlots[i]];
            totemNodes[i].setTranslateX(pos.x);
            totemNodes[i].setTranslateY(pos.y);
            totemNodes[i].setScaleX(pos.scale);
            totemNodes[i].setScaleY(pos.scale);
            totemNodes[i].setViewOrder(-pos.scale);

            if (nodeSlots[i] == 0) {
                totemNodes[i].setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 15, 0, 0, 10);");
            }

            carouselContainer.getChildren().add(totemNodes[i]);
        }

        pulseTransition = new ScaleTransition(Duration.millis(800));
        pulseTransition.setFromX(1.0);
        pulseTransition.setFromY(1.0);
        pulseTransition.setToX(1.04);
        pulseTransition.setToY(1.04);
        pulseTransition.setAutoReverse(true);
        pulseTransition.setCycleCount(ScaleTransition.INDEFINITE);

        HBox arrows = new HBox(50);
        arrows.setAlignment(Pos.CENTER);

        leftBtn = new Button("<");
        leftBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 36px; -fx-base: #3e2a1d; -fx-text-fill: #F2D5A3; -fx-cursor: hand;");
        leftBtn.setPrefSize(80, 60);
        leftBtn.setOnAction(e -> playCircularAnimation(-1));

        Button selectTotemBtn = new Button("SELECT THIS TOTEM");
        selectTotemBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 24px; -fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
        selectTotemBtn.setPrefSize(350, 60);
        selectTotemBtn.setOnAction(e -> { //[cite: 7]
            String finalName = nameInput.getText(); //[cite: 7]
            controller.requestSetUsername(finalName);});

        selectTotemBtn.setOnAction(e -> {
            String finalName = nameInput.getText();
            String finalColor = totemColors.get(currentTotemIndex);
        });

        rightBtn = new Button(">");
        rightBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 36px; -fx-base: #3e2a1d; -fx-text-fill: #F2D5A3; -fx-cursor: hand;");
        rightBtn.setPrefSize(80, 60);
        rightBtn.setOnAction(e -> playCircularAnimation(1));

        arrows.getChildren().addAll(leftBtn, selectTotemBtn, rightBtn);
        totemPhaseBox.getChildren().addAll(title, currentColorLabel, carouselContainer, arrows);
    }

    private void playCircularAnimation(int direction) {
        leftBtn.setDisable(true);
        rightBtn.setDisable(true);
        stopPulseAnimation();

        Timeline timeline = new Timeline();

        for (int i = 0; i < 5; i++) {
            nodeSlots[i] = (nodeSlots[i] + direction + 5) % 5;
            CarouselPosition target = slots[nodeSlots[i]];
            ImageView node = totemNodes[i];

            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(400),
                    new KeyValue(node.translateXProperty(), target.x, Interpolator.EASE_BOTH),
                    new KeyValue(node.translateYProperty(), target.y, Interpolator.EASE_BOTH),
                    new KeyValue(node.scaleXProperty(), target.scale, Interpolator.EASE_BOTH),
                    new KeyValue(node.scaleYProperty(), target.scale, Interpolator.EASE_BOTH),
                    new KeyValue(node.viewOrderProperty(), -target.scale, Interpolator.EASE_BOTH)
            ));

            if (nodeSlots[i] == 0) {
                currentTotemIndex = i;
                currentColorLabel.setText(totemColors.get(i));
                node.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 15, 0, 0, 10);");
            } else {
                node.setStyle("");
            }
        }

        timeline.setOnFinished(e -> {
            leftBtn.setDisable(false);
            rightBtn.setDisable(false);
            startPulseAnimation();
        });

        timeline.play();
    }

    private void startPulseAnimation() {
        for (int i = 0; i < 5; i++) {
            if (nodeSlots[i] == 0) {
                pulseTransition.setNode(totemNodes[i]);
                pulseTransition.play();
                break;
            }
        }
    }

    private void stopPulseAnimation() {
        if (pulseTransition.getNode() != null) {
            pulseTransition.stop();
            pulseTransition.getNode().setScaleX(1.0);
            pulseTransition.getNode().setScaleY(1.0);
        }
    }

    private void switchToTotemPhase() {
        nicknamePhaseBox.setVisible(false);
        totemPhaseBox.setVisible(true);
        startPulseAnimation();
    }

    private AnchorPane createBottomControls() {
        AnchorPane bottomPane = new AnchorPane();
        bottomPane.setPadding(new Insets(30));

        Button returnBtn = new Button("RETURN");
        returnBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 20px; -fx-base: #8B0000; -fx-text-fill: white; -fx-cursor: hand;");
        AnchorPane.setBottomAnchor(returnBtn, 0.0);
        AnchorPane.setLeftAnchor(returnBtn, 0.0);
        returnBtn.setOnAction(e -> controller.showLobbyListScene());

        Button quitBtn = new Button("QUIT");
        quitBtn.setStyle("-fx-font-family: '" + customFontFamily + "'; -fx-font-size: 20px; -fx-base: #555555; -fx-text-fill: white; -fx-cursor: hand;");
        AnchorPane.setBottomAnchor(quitBtn, 0.0);
        AnchorPane.setRightAnchor(quitBtn, 0.0);
        quitBtn.setOnAction(e -> {
            if (controller != null) controller.disconnect();
            System.exit(0);
        });

        bottomPane.getChildren().addAll(returnBtn, quitBtn);
        bottomPane.setPickOnBounds(false);

        return bottomPane;
    }

    private static class CarouselPosition {
        final double x, y, scale;
        CarouselPosition(double x, double y, double scale) {
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }
}