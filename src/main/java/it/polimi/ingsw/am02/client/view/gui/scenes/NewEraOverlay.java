package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.common.enumerations.Era;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NewEraOverlay {

    private static final int CANVAS_W = 800;
    private static final int CANVAS_H = 500;
    private static final int PARTICLE_COUNT = 220;

    private final List<FireParticle> particles = new ArrayList<>();
    private final Random rng = new Random();
    private AnimationTimer fireTimer;

    private static class FireParticle {
        double x, y, vx, vy, life, maxLife, size;
        int type; // 0=core, 1=ember, 2=smoke

        FireParticle() { reset(0, 0, true, new Random()); }

        void reset(double originX, double originY, boolean initial, Random rng) {
            x = originX + (rng.nextDouble() - 0.5) * 340;
            y = initial ? originY - rng.nextDouble() * CANVAS_H * 0.6 : originY + 10;
            double speed = 0.8 + rng.nextDouble() * 2.2;
            double angle  = -Math.PI / 2 + (rng.nextDouble() - 0.5) * 1.1;
            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed;
            maxLife = 0.6 + rng.nextDouble() * 1.4;
            life = initial ? rng.nextDouble() * maxLife : maxLife;
            size = 4 + rng.nextDouble() * 14;
            type = rng.nextDouble() < 0.05 ? 2 : (rng.nextDouble() < 0.25 ? 1 : 0);
        }
    }

    /**
     * Costruisce il nodo da inserire nel modalLayer del GuiController.
     * @param era    la nuova era
     * @param onDone callback richiamata quando l'animazione termina
     */
    public StackPane buildNode(Era era, Runnable onDone) {

        Canvas fireCanvas = new Canvas(CANVAS_W, CANVAS_H);
        fireCanvas.setMouseTransparent(true);
        GraphicsContext gc = fireCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, CANVAS_W, CANVAS_H);
        gc.setFill(Color.color(0, 0, 0, 0.08));  // trail leggerissimo, quasi invisibile
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);

        Text eraText = new Text(eraRoman(era));
        eraText.setFont(Font.font("Georgia", FontWeight.BOLD, 130));
        eraText.setFill(Color.TRANSPARENT);
        eraText.setStroke(Color.TRANSPARENT);
        eraText.setTextAlignment(TextAlignment.CENTER);
        DropShadow eraGlow = new DropShadow(BlurType.GAUSSIAN, Color.color(1, 0.4, 0, 1), 40, 0.6, 0, 0);
        eraText.setEffect(eraGlow);

        Text subtitle = new Text("NEW ERA HAS BEGUN");
        subtitle.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        subtitle.setFill(Color.color(1, 0.85, 0.3, 0));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        DropShadow subGlow = new DropShadow(BlurType.GAUSSIAN, Color.color(1, 0.5, 0, 0.9), 20, 0.5, 0, 0);
        subtitle.setEffect(subGlow);

        VBox textBox = new VBox(6, eraText, subtitle);
        textBox.setAlignment(Pos.CENTER);
        textBox.setMouseTransparent(true);

        StackPane root = new StackPane(fireCanvas, textBox);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: transparent;");

        double originX = CANVAS_W / 2.0;
        double originY = CANVAS_H * 0.72;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            FireParticle p = new FireParticle();
            p.reset(originX, originY, true, rng);
            particles.add(p);
        }

        fireTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                renderFire(fireCanvas.getGraphicsContext2D(), originX, originY);
            }
        };

        // --- Animazioni ---

        FadeTransition bgFade = new FadeTransition(Duration.millis(400));
        bgFade.setFromValue(0); bgFade.setToValue(0.82);

        Timeline eraReveal = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(eraText.fillProperty(),        Color.color(1, 0.2, 0, 0)),
                        new KeyValue(eraText.strokeProperty(),      Color.color(1, 0.6, 0, 0)),
                        new KeyValue(eraText.strokeWidthProperty(), 0)
                ),
                new KeyFrame(Duration.millis(700),
                        new KeyValue(eraText.fillProperty(),        Color.color(1, 0.55, 0, 1)),
                        new KeyValue(eraText.strokeProperty(),      Color.color(1, 0.85, 0.1, 1)),
                        new KeyValue(eraText.strokeWidthProperty(), 2)
                )
        );

        Timeline eraPulse = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(eraGlow.radiusProperty(), 35)),
                new KeyFrame(Duration.millis(700),  new KeyValue(eraGlow.radiusProperty(), 65)),
                new KeyFrame(Duration.millis(1400), new KeyValue(eraGlow.radiusProperty(), 35))
        );
        eraPulse.setCycleCount(Animation.INDEFINITE);
        eraPulse.setAutoReverse(true);

        FadeTransition subFade = new FadeTransition(Duration.millis(500), subtitle);
        subFade.setFromValue(0); subFade.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(600), textBox);
        scaleIn.setFromX(0.4); scaleIn.setFromY(0.4);
        scaleIn.setToX(1.0);   scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.4, 1.0));

        TranslateTransition shake = new TranslateTransition(Duration.millis(60), textBox);
        shake.setFromX(0); shake.setToX(8);
        shake.setCycleCount(6); shake.setAutoReverse(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), root);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> { fireTimer.stop(); particles.clear(); if (onDone != null) onDone.run(); });

        new SequentialTransition(
                new PauseTransition(Duration.millis(1)),
                new ParallelTransition(eraReveal, scaleIn),
                new PauseTransition(Duration.millis(200)),
                subFade,
                new PauseTransition(Duration.millis(1800)),
                shake,
                fadeOut
        ).play();

        eraPulse.play();
        fireTimer.start();
        return root;
    }

    private void renderFire(GraphicsContext gc, double originX, double originY) {
        gc.clearRect(0, 0, CANVAS_W, CANVAS_H);  // cancella trasparente invece di riempire di nero

        for (FireParticle p : particles) {
            p.life -= 0.012;
            if (p.life <= 0) { p.reset(originX, originY, false, rng); continue; }

            double t = 1.0 - (p.life / p.maxLife);
            p.vy -= 0.04;
            p.vx += (rng.nextDouble() - 0.5) * 0.12;
            p.x  += p.vx;
            p.y  += p.vy;
            double alpha = Math.sin(t * Math.PI);

            if (p.type == 2) {
                double r = p.size * (1 + t * 2);
                gc.setFill(Color.rgb(80, 70, 60, alpha * 0.25));
                gc.fillOval(p.x - r, p.y - r, r * 2, r * 2);
            } else if (p.type == 1) {
                double r = p.size * 0.35 * (1 - t * 0.5);
                gc.setFill(Color.rgb(255, 240, 180, alpha));
                gc.fillOval(p.x - r, p.y - r, r * 2, r * 2);
            } else {
                Color c = fireColor(t);
                double r = p.size * (1 - t * 0.6);
                gc.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha * 0.9));
                gc.fillOval(p.x - r, p.y - r, r * 2, r * 2);
                gc.setFill(Color.color(c.getRed(), c.getGreen() * 0.5, 0, alpha * 0.25));
                gc.fillOval(p.x - r * 1.8, p.y - r * 1.8, r * 3.6, r * 3.6);
            }
        }

        gc.setFill(new RadialGradient(0, 0, originX / CANVAS_W, originY / CANVAS_H, 0.35, true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1.0, 0.4, 0.0, 0.30)),
                new Stop(0.5, Color.color(1.0, 0.1, 0.0, 0.10)),
                new Stop(1.0, Color.TRANSPARENT)));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
    }

    private Color fireColor(double t) {
        if (t < 0.3)  { double f = t / 0.3;        return Color.color(1.0, 1.0 - f*0.3, 1.0 - f, 1.0); }
        if (t < 0.65) { double f = (t-0.3) / 0.35; return Color.color(1.0, 0.7 - f*0.4, 0.0,     1.0); }
        else          { double f = (t-0.65)/ 0.35;  return Color.color(1.0 - f*0.5, 0.3 - f*0.25, 0.0, 1.0); }
    }

    private String eraRoman(Era era) {
        return switch (era) { case I -> "ERA I"; case II -> "ERA II"; case III -> "ERA III"; };
    }
}