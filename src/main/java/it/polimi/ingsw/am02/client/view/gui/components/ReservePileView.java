package it.polimi.ingsw.am02.client.view.gui.components;

import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Graphical representation of the common reserve in the GUI.
 * <p>
 * Mirrors the "riserva comune" of the physical board game: two heaps of tokens
 * (prestige points on top, food below) shown to the right of the offer track.
 * The reserve is intentionally <em>infinite</em> &mdash; there is no server-side
 * supply to track &mdash; so each heap is purely decorative and is annotated with
 * an infinity glyph rather than a count.
 */
public class ReservePileView extends VBox {

    private static final String DIR = "/images/icons/reserve/";

    /** A single token placement within a heap: image + (x, y) offset from centre. */
    private record Token(String file, double dx, double dy) {}

    /**
     * Prestige heap: every denomination (positive gold, negative red), scattered
     * back-to-front so the heap reads as a loose pile.
     */
    private static final Token[] PRESTIGE = {
            new Token("pp_p5.png", -30, -12), new Token("pp_p10.png", -10, -14),
            new Token("pp_p20.png", 12, -12), new Token("pp_m5.png", 32, -10),
            new Token("pp_p1.png", -34, 2),   new Token("pp_p3.png", -14, 0),
            new Token("pp_m20.png", 8, 1),    new Token("pp_m10.png", 28, 2),
            new Token("pp_m1.png", -22, 13),  new Token("pp_m3.png", 2, 12)
    };

    /** Food heap: both denominations (meat "1" and fish "5") mixed into a pile. */
    private static final Token[] FOOD = {
            new Token("food_5.png", -20, -8), new Token("food_1.png", 4, -9),
            new Token("food_5.png", 24, -7),  new Token("food_1.png", -26, 9),
            new Token("food_5.png", -2, 10),  new Token("food_1.png", 20, 9)
    };

    private static final double PP_TOKEN_H = 30;
    private static final double FOOD_TOKEN_H = 34;

    /**
     * Constructs the reserve view.
     *
     * @param tribalFont the font used for the title and infinity glyph; may be {@code null}.
     */
    public ReservePileView(Font tribalFont) {
        super(2);
        this.setAlignment(Pos.TOP_CENTER);
        this.setPadding(new Insets(8, 12, 8, 12));
        this.setStyle("-fx-background-color: rgba(26, 15, 7, 0.55);"
                + " -fx-background-radius: 8;"
                + " -fx-border-color: #3e2a1d; -fx-border-width: 1; -fx-border-radius: 8;");

        Label title = new Label("RESERVE");
        title.setTextFill(Color.web("#F2D5A3"));
        if (tribalFont != null) title.setFont(Font.font(tribalFont.getFamily(), 12));

        this.getChildren().addAll(
                title,
                heap(PRESTIGE, PP_TOKEN_H, 124, 62, tribalFont),
                heap(FOOD, FOOD_TOKEN_H, 100, 54, tribalFont)
        );
    }

    /**
     * Builds a decorative heap of overlapping tokens topped with an infinity glyph
     * to convey that the supply is unlimited. Tokens are drawn in array order, so
     * earlier entries sit behind later ones.
     *
     * @param tokens     the token placements.
     * @param tokenH     display height of each token.
     * @param w          preferred heap width.
     * @param h          preferred heap height.
     * @param tribalFont font for the infinity glyph; may be {@code null}.
     * @return a {@link StackPane} containing the heap.
     */
    private StackPane heap(Token[] tokens, double tokenH, double w, double h, Font tribalFont) {
        StackPane pile = new StackPane();
        pile.setPrefSize(w, h);
        pile.setMinSize(w, h);

        for (Token t : tokens) {
            ImageView iv = new ImageView(ImageLoader.getImage(DIR + t.file()));
            iv.setFitHeight(tokenH);
            iv.setPreserveRatio(true);
            iv.setTranslateX(t.dx());
            iv.setTranslateY(t.dy());
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 4, 0, 1, 1);");
            pile.getChildren().add(iv);
        }

        Label infinity = new Label("∞");
        infinity.setTextFill(Color.WHITE);
        infinity.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 0 5; -fx-background-radius: 8;");
        if (tribalFont != null) infinity.setFont(Font.font(tribalFont.getFamily(), 13));
        StackPane.setAlignment(infinity, Pos.BOTTOM_RIGHT);
        infinity.setTranslateX(8);
        infinity.setTranslateY(6);
        pile.getChildren().add(infinity);

        return pile;
    }
}
