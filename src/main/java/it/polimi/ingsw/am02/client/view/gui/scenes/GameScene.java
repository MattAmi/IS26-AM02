package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.view.CardCatalog;
import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameScene {

    private GuiController controller;
    private GameModel model;
    private BorderPane root;

    private VBox cardsArea;
    private HBox trackArea;
    private VBox statusArea;
    
    private final List<String> selected = new ArrayList<>();

    public Region buildNode(GuiController controller) {
        this.controller = controller;
        root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");

        // Top: Stats
        statusArea = new VBox(5);
        statusArea.setPadding(new Insets(10));
        statusArea.setStyle("-fx-background-color: #2b1d14; -fx-border-color: #F2D5A3; -fx-border-width: 0 0 2 0;");
        root.setTop(statusArea);

        // Center: Cards
        cardsArea = new VBox(20);
        cardsArea.setPadding(new Insets(20));
        ScrollPane scroll = new ScrollPane(cardsArea);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scroll);

        // Bottom: Controls
        VBox bottom = new VBox(10);
        bottom.setPadding(new Insets(10));
        bottom.setStyle("-fx-background-color: #2b1d14;");

        trackArea = new HBox(15);
        trackArea.setAlignment(Pos.CENTER);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button confirm = new Button("CONFIRM PICK");
        confirm.setOnAction(e -> {
            controller.resolveActions(new ArrayList<>(selected));
            selected.clear();
        });

        Button endTurn = new Button("END PLAYER TURN (MOVE T)");
        endTurn.setOnAction(e -> controller.moveTotem('T'));

        Button summaryBtn = new Button("📋 Riepilogo");
        summaryBtn.setStyle(
                "-fx-background-color: #5C6B32; -fx-text-fill: white; -fx-font-weight: bold;"
        );
        summaryBtn.setOnAction(e -> controller.showSummaryCard());

        // AGGIUNTA CORRETTA: Tutti e tre i bottoni aggiunti una volta sola!
        actions.getChildren().addAll(confirm, endTurn, summaryBtn);

        bottom.getChildren().addAll(trackArea, actions);
        root.setBottom(bottom);

        return root;
    }

    public void refreshAll(GameModel model) {
        this.model = model;
        Platform.runLater(() -> {
            if (model == null) return;
            
            // Stats
            statusArea.getChildren().clear();
            Label l1 = new Label("GAME: " + model.getGameId() + " | PHASE: " + model.getCurrentPhase());
            l1.setTextFill(Color.web("#F2D5A3"));
            Label l2 = new Label("ACTIVE PLAYER: " + model.getCurrentPlayer() + " | MY NICK: " + model.getMyNickname());
            l2.setTextFill(Color.WHITE);
            statusArea.getChildren().addAll(l1, l2);

            // Cards
            cardsArea.getChildren().clear();
            addCardRow("UPPER ROW", model.getUpperRow());
            addCardRow("LOWER ROW", model.getLowerRow());
            addCardRow("UPPER BUILDINGS", model.getUpperRowBuildings());
            addCardRow("LOWER BUILDINGS", model.getLowerRowBuildings());

            // Track
            trackArea.getChildren().clear();
            for (OfferTileInfo t : model.getOfferTiles()) {
                trackArea.getChildren().add(createTileView(t));
            }
        });
    }

    private void addCardRow(String title, List<String> ids) {
        if (ids.isEmpty()) return;
        VBox row = new VBox(5);
        Label lbl = new Label(title); lbl.setTextFill(Color.GRAY);
        FlowPane fp = new FlowPane(10, 10);
        for (String id : ids) fp.getChildren().add(createCardBox(id));
        row.getChildren().addAll(lbl, fp);
        cardsArea.getChildren().add(row);
    }

    private VBox createCardBox(String id) {
        VBox b = new VBox(5);
        b.setPadding(new Insets(10));
        boolean sel = selected.contains(id);
        b.setStyle("-fx-border-color: " + (sel ? "GOLD" : "#444") + "; -fx-background-color: " + (sel ? "#3a4a3a" : "#2a2a2a") + "; -fx-border-radius: 5;");
        b.setPrefSize(120, 160);
        Label idL = new Label(id); idL.setTextFill(Color.WHITE);
        Label dL = new Label(CardCatalog.getInstance().format(id)); dL.setWrapText(true); dL.setTextFill(Color.LIGHTGRAY); dL.setFont(Font.font(10));
        b.getChildren().addAll(idL, dL);
        b.setOnMouseClicked(e -> {
            if(selected.contains(id)) selected.remove(id); else selected.add(id);
            refreshAll(model);
        });
        return b;
    }

    private VBox createTileView(OfferTileInfo t) {
        VBox b = new VBox(2); b.setAlignment(Pos.CENTER); b.setPrefSize(70, 70);
        boolean mine = model.getMyNickname().equals(t.occupantNickname());
        b.setStyle("-fx-border-color: " + (mine ? "LIME" : "#F2D5A3") + "; -fx-background-color: #3e2a1d;");
        Label l = new Label(String.valueOf(t.tileID())); l.setTextFill(Color.WHITE);
        Label p = new Label(t.upperChoosable() + "/" + t.lowerChoosable()); p.setTextFill(Color.LIGHTGRAY); p.setFont(Font.font(9));
        b.getChildren().addAll(l, p);
        if (t.occupantNickname() != null) {
            Label o = new Label(t.occupantNickname()); o.setTextFill(Color.GOLD); o.setFont(Font.font(9));
            b.getChildren().add(o);
        }
        b.setOnMouseClicked(e -> controller.moveTotem(t.tileID()));
        return b;
    }

    // Stub handlers
    public void setupInitialBoard(List<String> t, Map<String, Integer> f, BoardSnapshot b) { refreshAll(model); }
    public void updatePhase(PhaseType p, String c, List<String> r) { refreshAll(model); }
    public void highlightCurrentPlayer(String n) { refreshAll(model); }
    public void updateTurnOrder(List<String> t) { refreshAll(model); }
    public void animateTotemPlacement(String n, char t) { refreshAll(model); }
    public void animateTotemReturn(String n, int p) { refreshAll(model); }
    public void updateOfferTiles(List<OfferTileInfo> o) { refreshAll(model); }
    public void updateBoardCards(List<String> u, List<String> l, int d) { refreshAll(model); }
    public void showNewEraAnimation(List<String> u, List<String> l) { refreshAll(model); }
    public void updatePlayerLimits(String n, int u, int l) { refreshAll(model); }
    public void updatePlayerResource(String n, ResourceType r, int v) { refreshAll(model); }
    public void animateCardTaken(String n, String c, CardType t, RowPosition s) { refreshAll(model); }
    public void showEventResolved(String id, String name) {}
    public void showExtraTurnAlert(String n, int u, int l) {}
    public void endExtraTurn(String n) { refreshAll(model); }
    public void setPlayerOffline(String n) { refreshAll(model); }
    public void setPlayerOnline(String n) { refreshAll(model); }
}
