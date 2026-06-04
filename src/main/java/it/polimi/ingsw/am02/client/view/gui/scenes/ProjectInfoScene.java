package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.common.ProjectInfo;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.Map;
import java.util.Objects;
/**
 * Represents the project information scene.
 * This scene displays details about the group, course, university, and project members.
 */
public class ProjectInfoScene {

    /**
     * Builds the project information scene node and handles the fade-in and auto-advance logic.
     *
     * @param onFinished Callback to execute when the scene auto-advances.
     * @return The constructed Region representing the scene.
     */
    public Region buildNode(Runnable onFinished) {
        Font introTitle = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 48);
        Font introText = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 18);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        ImageView backgroundView = new ImageView();
        try {
            Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/it.polimi.ingsw.am02.images/mesos_lobby.png")));
            backgroundView.setImage(img);
        } catch (Exception ignored) {}

        backgroundView.fitWidthProperty().bind(root.widthProperty());
        backgroundView.fitHeightProperty().bind(root.heightProperty());
        backgroundView.setPreserveRatio(false);

        ScaleTransition stBg = new ScaleTransition(Duration.seconds(5), backgroundView);
        stBg.setFromX(1.0);
        stBg.setFromY(1.0);
        stBg.setToX(1.18);
        stBg.setToY(1.18);
        stBg.play();

        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");

        VBox contentBox = new VBox(40);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(50));
        contentBox.setMaxWidth(800);
        contentBox.setMouseTransparent(true);

        Text titleText = new Text("PROJECT INFO");
        titleText.setFill(Color.web("#F2D5A3"));
        if (introTitle != null) titleText.setFont(introTitle);

        TextFlow titleFlow = new TextFlow(titleText);
        titleFlow.setTextAlignment(TextAlignment.CENTER);

        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append(ProjectInfo.GROUP).append("\n");
        infoBuilder.append(ProjectInfo.COURSE).append("\n");
        infoBuilder.append(ProjectInfo.PROFESSOR).append("\n");
        infoBuilder.append(ProjectInfo.UNIVERSITY).append("\n");
        infoBuilder.append(ProjectInfo.YEAR).append("\n\n");

        for (Map.Entry<String, String> member : ProjectInfo.MEMBERS.entrySet()) {
            infoBuilder.append(member.getKey()).append(" - ").append(member.getValue()).append("\n");
        }

        Text infoContent = new Text(infoBuilder.toString().trim());
        infoContent.setFill(Color.WHITE);
        if (introText != null) infoContent.setFont(introText);

        TextFlow textFlow = new TextFlow(infoContent);
        textFlow.setTextAlignment(TextAlignment.CENTER);
        textFlow.setStyle("-fx-background-color: rgba(43, 29, 20, 0.4); -fx-border-color: #F2D5A3; -fx-border-width: 1; -fx-border-radius: 10; -fx-padding: 30;");

        contentBox.getChildren().addAll(titleFlow, textFlow);

        contentBox.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), contentBox);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        root.getChildren().addAll(backgroundView, overlay, contentBox);

        PauseTransition autoAdvance = new PauseTransition(Duration.seconds(7));
        autoAdvance.setOnFinished(e -> onFinished.run());
        autoAdvance.play();

        return root;
    }
}