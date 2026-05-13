package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.view.CardCatalog;
import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameScene {
    private GuiController controller;
    private GameModel model;
    private StackPane baseStack;
    private BorderPane root;
    private TextFlow statusFlow;
    private VBox rightSidebar;
    private VBox mainBoardArea;
    private HBox handCardsBox;
    private Label handTitle;
    private String viewedPlayerHand;
    private Font tribalFont;
    private StackPane centerLayout;
    private ImageView currentDeckView;
    private Label gameIdLabel;

    private final List<String> selected = new ArrayList<>();
    private final List<String> offlinePlayers = new ArrayList<>();
    private final List<String> knownCardsOnBoard = new ArrayList<>();
    private final List<DealTask> pendingDeals = new ArrayList<>();

    private int currentEra = 1;

    private static class DealTask {
        String id;
        ImageView target;
        DealTask(String id, ImageView target) {
            this.id = id;
            this.target = target;
        }
    }

    public Region buildNode(GuiController controller) {
        this.controller = controller;
        this.tribalFont = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 14);
        if (tribalFont == null) tribalFont = Font.font("System", 14);

        baseStack = new StackPane();
        baseStack.setMinWidth(1280);
        baseStack.setMinHeight(800);
        baseStack.setStyle("-fx-background-color: #0a0a0a;");

        root = new BorderPane();

        StackPane topBanner = new StackPane();
        topBanner.setPadding(new Insets(10, 20, 10, 20));
        topBanner.setStyle("-fx-background-color: #A31D1D;");

        HBox idBox = new HBox(10);
        idBox.setAlignment(Pos.CENTER_LEFT);

        gameIdLabel = new Label("ID: ---");
        gameIdLabel.setTextFill(Color.WHITE);
        gameIdLabel.setFont(Font.font(tribalFont.getFamily(), 14));

        Button copyBtn = new Button("📋");
        copyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 0; -fx-font-size: 16;");
        copyBtn.setTooltip(new Tooltip("Copy Game ID"));
        copyBtn.setOnAction(e -> {
            if (model != null && model.getGameId() != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(model.getGameId());
                clipboard.setContent(content);
            }
        });

        idBox.getChildren().addAll(gameIdLabel, copyBtn);
        StackPane.setAlignment(idBox, Pos.CENTER_LEFT);

        Button burgerMenuBtn = new Button("☰");
        burgerMenuBtn.setFont(Font.font(tribalFont.getFamily(), 24));
        burgerMenuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        burgerMenuBtn.setOnAction(e -> controller.showInGameMenu());
        StackPane.setAlignment(burgerMenuBtn, Pos.CENTER_RIGHT);

        statusFlow = new TextFlow();
        statusFlow.setTextAlignment(TextAlignment.CENTER);
        statusFlow.setMouseTransparent(true);

        topBanner.getChildren().addAll(statusFlow, idBox, burgerMenuBtn);
        root.setTop(topBanner);

        rightSidebar = new VBox(15);
        rightSidebar.setPadding(new Insets(20));
        rightSidebar.setPrefWidth(260);
        rightSidebar.setStyle(
                "-fx-background-color: #1a0f07; " +
                        "-fx-border-color: #3e2a1d; " +
                        "-fx-border-width: 0 0 0 4;"
        );
        root.setRight(rightSidebar);

        centerLayout = new StackPane();
        String bgPath = getClass().getResource("/it.polimi.ingsw.am02.images/mesos_box.png").toExternalForm();
        centerLayout.setStyle(
                "-fx-background-image: url('" + bgPath + "'); " +
                        "-fx-background-size: 130%; " +
                        "-fx-background-position: center;"
        );

        Region darkOverlay = new Region();
        darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");

        mainBoardArea = new VBox(30);
        mainBoardArea.setPadding(new Insets(20, 20, 250, 20));
        mainBoardArea.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(mainBoardArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox bottomLayout = new VBox(0);
        bottomLayout.setMaxHeight(Region.USE_PREF_SIZE);
        bottomLayout.setAlignment(Pos.BOTTOM_CENTER);

        VBox handArea = new VBox(5);
        handArea.setPadding(new Insets(10));
        handArea.setStyle("-fx-background-color: rgba(26, 15, 7, 0.4); -fx-border-color: #F2D5A3; -fx-border-width: 1 0 1 0;");

        handTitle = new Label("TRIBE");
        handTitle.setTextFill(Color.web("#F2D5A3"));
        handTitle.setFont(Font.font(tribalFont.getFamily(), 14));

        handCardsBox = new HBox(10);
        handCardsBox.setAlignment(Pos.CENTER_LEFT);
        handCardsBox.setMinHeight(180);
        handCardsBox.setPrefHeight(180);

        ScrollPane handScroll = new ScrollPane(handCardsBox);
        handScroll.setFitToHeight(true);
        handScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        handScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        handArea.getChildren().addAll(handTitle, handScroll);

        HBox bottomButtons = new HBox(25);
        bottomButtons.setPadding(new Insets(15));
        bottomButtons.setAlignment(Pos.CENTER);
        bottomButtons.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        String buttonStyle = "-fx-base: #2c1a0e; -fx-text-fill: white; -fx-cursor: hand; -fx-font-family: \"" + tribalFont.getFamily() + "\"; -fx-font-weight: bold; -fx-border-color: #4A3B32; -fx-border-radius: 3;";

        Button confirmBtn = new Button("CONFIRM PICK");
        confirmBtn.setFont(Font.font(tribalFont.getFamily(), 16));
        confirmBtn.setStyle(buttonStyle);
        confirmBtn.setOnAction(e -> { controller.resolveActions(new ArrayList<>(selected)); selected.clear(); });

        Button endTurnBtn = new Button("END TURN");
        endTurnBtn.setFont(Font.font(tribalFont.getFamily(), 16));
        endTurnBtn.setStyle(buttonStyle);
        endTurnBtn.setOnAction(e -> controller.moveTotem('T'));

        Button summaryBtn = new Button("SUMMARY");
        summaryBtn.setFont(Font.font(tribalFont.getFamily(), 16));
        summaryBtn.setStyle(buttonStyle);
        summaryBtn.setOnAction(e -> controller.showSummaryCard());

        bottomButtons.getChildren().addAll(confirmBtn, endTurnBtn, summaryBtn);
        bottomLayout.getChildren().addAll(handArea, bottomButtons);

        StackPane.setAlignment(bottomLayout, Pos.BOTTOM_CENTER);
        centerLayout.getChildren().addAll(darkOverlay, scrollPane, bottomLayout);

        root.setCenter(centerLayout);

        baseStack.getChildren().add(root);
        return baseStack;
    }

    public void refreshAll(GameModel model) {
        if (model == null) return;
        this.model = model;
        if (viewedPlayerHand == null) viewedPlayerHand = model.getMyNickname();

        Platform.runLater(() -> {
            gameIdLabel.setText("ID: " + (model.getGameId() != null ? model.getGameId() : "---"));
            updateStatusBanner();
            updateSidebar();
            updateMainBoard();
            updateHandDisplay();

            root.applyCss();
            root.layout();

            if (!pendingDeals.isEmpty()) {
                for (DealTask task : pendingDeals) {
                    animateDeal(task.id, task.target);
                }
                pendingDeals.clear();
            }
        });
    }

    private void updateStatusBanner() {
        statusFlow.getChildren().clear();
        String activePlayer = model.getCurrentPlayer() != null ? model.getCurrentPlayer().toUpperCase() : "...";
        String currentPhase = model.getCurrentPhase() != null ? model.getCurrentPhase().toString().replace("_", " ") : "WAITING";

        Text eraTxt = new Text("ERA " + currentEra + "  |  ");
        eraTxt.setFill(Color.WHITE);
        eraTxt.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 20));

        Text phaseTxt = new Text(currentPhase + "  |  ");
        phaseTxt.setFill(Color.LIGHTGRAY);
        phaseTxt.setFont(Font.font(tribalFont.getFamily(), 16));

        Text p1 = new Text("PLAYER ");
        p1.setFill(Color.WHITE);
        p1.setFont(Font.font(tribalFont.getFamily(), 16));

        Text p2 = new Text(activePlayer);
        p2.setFill(Color.GOLD);
        p2.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 22));

        statusFlow.getChildren().addAll(eraTxt, phaseTxt, p1, p2);
    }

    private void updateSidebar() {
        rightSidebar.getChildren().clear();
        Label sidebarTitle = new Label("PLAYERS");
        sidebarTitle.setTextFill(Color.WHITE);
        sidebarTitle.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 20));
        sidebarTitle.setMaxWidth(Double.MAX_VALUE);
        sidebarTitle.setAlignment(Pos.CENTER);
        rightSidebar.getChildren().add(sidebarTitle);

        List<String> players = model.getTurnOrder();
        if (players == null || players.isEmpty() || (!players.isEmpty() && players.get(0).matches("\\d+"))) {
            if (model.getFoodByPlayer() != null) players = new ArrayList<>(model.getFoodByPlayer().keySet());
        }

        if (players != null) {
            for (String nick : players) {
                VBox pBox = new VBox(5);
                pBox.setPadding(new Insets(10));
                pBox.setCursor(Cursor.HAND);
                boolean isActive = nick.equals(model.getCurrentPlayer());
                boolean isOffline = offlinePlayers.contains(nick);
                String borderColor = nick.equals(viewedPlayerHand) ? "#F2D5A3" : "transparent";

                pBox.setStyle("-fx-background-color: " + (isActive ? "rgba(163, 29, 29, 0.3)" : "rgba(255,255,255,0.05)") +
                        "; -fx-border-color: " + borderColor + "; -fx-border-radius: 5; -fx-border-width: 2;");
                pBox.setOpacity(isOffline ? 0.5 : 1.0);

                Label nameL = new Label(nick.toUpperCase() + (nick.equals(model.getMyNickname()) ? " (YOU)" : ""));
                nameL.setTextFill(isActive ? Color.GOLD : Color.WHITE);
                nameL.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 15));

                int food = model.getFoodByPlayer().getOrDefault(nick, 0);
                int pp = model.getPpByPlayer().getOrDefault(nick, 0);
                Label statsL = new Label("Food: " + food + " | PP: " + pp);
                statsL.setTextFill(Color.LIGHTGRAY);
                statsL.setFont(Font.font(tribalFont.getFamily(), 12));

                pBox.getChildren().addAll(nameL, statsL);
                pBox.setOnMouseClicked(e -> { viewedPlayerHand = nick; refreshAll(model); });
                rightSidebar.getChildren().add(pBox);
            }
        }
    }

    private void updateMainBoard() {
        mainBoardArea.getChildren().clear();

        HBox upperBand = new HBox(40);
        upperBand.setAlignment(Pos.CENTER);
        upperBand.getChildren().addAll(createCardGroup(model.getUpperRowBuildings()), createCardGroup(model.getUpperRow()));

        HBox lowerBand = new HBox(40);
        lowerBand.setAlignment(Pos.CENTER);
        lowerBand.getChildren().addAll(createCardGroup(model.getLowerRowBuildings()), createCardGroup(model.getLowerRow()));

        mainBoardArea.getChildren().addAll(upperBand, createTrackArea(), lowerBand);
    }

    private HBox createTrackArea() {
        HBox track = new HBox(25); track.setAlignment(Pos.CENTER);
        int displayEra = Math.min(currentEra, 3);
        String eraPath = "/it.polimi.ingsw.am02.images/cards/eras/back_main_era_" + displayEra + ".png";
        currentDeckView = new ImageView(ImageLoader.getImage(eraPath));
        currentDeckView.setFitHeight(150); currentDeckView.setPreserveRatio(true);

        int numPlayers = model.getTurnOrder() != null ? model.getTurnOrder().size() : 2;
        ImageView turnTile = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/turn_order_tiles/tile_turn_" + numPlayers + "p.png"));
        turnTile.setFitHeight(150); turnTile.setPreserveRatio(true);

        HBox offerTiles = new HBox(5);
        if (model.getOfferTiles() != null) {
            for (OfferTileInfo t : model.getOfferTiles()) offerTiles.getChildren().add(createTileView(t));
        }
        track.getChildren().addAll(currentDeckView, turnTile, offerTiles);
        return track;
    }

    private void updateHandDisplay() {
        handCardsBox.getChildren().clear();
        handTitle.setText("TRIBE OF: " + viewedPlayerHand.toUpperCase());
        List<String> chars = model.getCharactersByPlayer().getOrDefault(viewedPlayerHand, List.of());
        List<String> buildings = model.getBuildingsByPlayer().getOrDefault(viewedPlayerHand, List.of());
        List<String> allCards = Stream.concat(chars.stream(), buildings.stream()).collect(Collectors.toList());
        for (String id : allCards) {
            VBox cardBox = createCardBox(id);
            ((ImageView) cardBox.getChildren().get(0)).setFitHeight(160);
            handCardsBox.getChildren().add(cardBox);
        }
    }

    private HBox createCardGroup(List<String> ids) {
        HBox hb = new HBox(12); hb.setAlignment(Pos.CENTER);
        if (ids != null) {
            for (String id : ids) {
                boolean isNew = !knownCardsOnBoard.contains(id);
                if (isNew) knownCardsOnBoard.add(id);
                VBox box = createCardBox(id);
                ImageView iv = (ImageView) box.getChildren().get(0);
                if (isNew) { iv.setOpacity(0.0); pendingDeals.add(new DealTask(id, iv)); }
                hb.getChildren().add(box);
            }
        }
        return hb;
    }

    private VBox createCardBox(String id) {
        VBox b = new VBox(); b.setCursor(Cursor.HAND);
        ImageView iv = new ImageView(ImageLoader.getImage(getCardPath(id)));
        iv.setFitHeight(170); iv.setPreserveRatio(true);
        if (selected.contains(id)) iv.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);");
        b.getChildren().add(iv);

        try {
            Tooltip tooltip = new Tooltip(CardCatalog.getInstance().format(id));
            tooltip.setWrapText(true);
            tooltip.setPrefWidth(250);
            tooltip.setShowDelay(Duration.seconds(1));
            Tooltip.install(b, tooltip);
        } catch (Exception ignored) { }

        b.setOnMouseEntered(e -> {
            iv.setTranslateY(-10); iv.setScaleX(1.1); iv.setScaleY(1.1);
            if (!selected.contains(id)) iv.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.7), 15, 0.4, 0, 0);");
        });
        b.setOnMouseExited(e -> {
            iv.setTranslateY(0); iv.setScaleX(1.0); iv.setScaleY(1.0);
            iv.setStyle(selected.contains(id) ? "-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);" : "");
        });
        b.setOnMouseClicked(e -> { if(selected.contains(id)) selected.remove(id); else selected.add(id); refreshAll(model); });
        return b;
    }

    private String getCardPath(String id) {
        String sub = id.startsWith("C_") ? "characters/" : id.startsWith("B_") ? "buildings/" : "events/";
        return "/it.polimi.ingsw.am02.images/cards/" + sub + id + ".png";
    }

    private StackPane createTileView(OfferTileInfo t) {
        StackPane st = new StackPane(); st.setCursor(Cursor.HAND);
        ImageView iv = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/offer_tiles/tile_offer_" + t.tileID() + ".png"));
        iv.setFitHeight(150); iv.setPreserveRatio(true);
        st.getChildren().add(iv);
        if (t.occupantNickname() != null) {
            Label nick = new Label(t.occupantNickname().substring(0, Math.min(3, t.occupantNickname().length())).toUpperCase());
            nick.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 2 5 2 5; -fx-background-radius: 3;");
            StackPane.setAlignment(nick, Pos.TOP_CENTER); StackPane.setMargin(nick, new Insets(10, 0, 0, 0));
            st.getChildren().add(nick);
        }
        st.setOnMouseClicked(e -> controller.moveTotem(t.tileID()));
        return st;
    }

    private void animateDeal(String id, ImageView target) {
        if (currentDeckView == null || currentDeckView.getScene() == null || target.getScene() == null) {
            target.setOpacity(1.0);
            return;
        }
        javafx.geometry.Point2D start = currentDeckView.localToScene(0, 0);
        javafx.geometry.Point2D end = target.localToScene(0, 0);
        int displayEra = Math.min(currentEra, 3);
        ImageView fly = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/eras/back_main_era_" + displayEra + ".png"));
        fly.setFitHeight(170); fly.setPreserveRatio(true); fly.setManaged(false);
        fly.relocate(start.getX(), start.getY());
        baseStack.getChildren().add(fly);

        TranslateTransition tt = new TranslateTransition(Duration.millis(700), fly);
        tt.setToX(end.getX() - start.getX()); tt.setToY(end.getY() - start.getY());

        ScaleTransition st1 = new ScaleTransition(Duration.millis(350), fly);
        st1.setToX(0);
        st1.setOnFinished(e -> {
            fly.setImage(ImageLoader.getImage(getCardPath(id)));
            ScaleTransition st2 = new ScaleTransition(Duration.millis(350), fly);
            st2.setToX(1.0); st2.play();
        });

        ParallelTransition pt = new ParallelTransition(tt, st1);
        pt.setOnFinished(e -> { baseStack.getChildren().remove(fly); target.setOpacity(1.0); });
        pt.play();
    }

    public void animateCardTaken(String nickname, String cardID, CardType type, RowPosition source) {
        Platform.runLater(() -> {
            ImageView fly = new ImageView(ImageLoader.getImage(getCardPath(cardID)));
            fly.setFitHeight(170); fly.setPreserveRatio(true);
            fly.setTranslateY(source == RowPosition.UPPER ? -150 : 150);
            baseStack.getChildren().add(fly);
            TranslateTransition tt = new TranslateTransition(Duration.seconds(1), fly);
            if (nickname.equals(model.getMyNickname())) { tt.setToY(350); tt.setToX(-200); }
            else { tt.setToY(-250); tt.setToX(500); }
            ScaleTransition st = new ScaleTransition(Duration.seconds(1), fly); st.setToX(0.1); st.setToY(0.1);
            FadeTransition ft = new FadeTransition(Duration.seconds(1), fly); ft.setToValue(0);
            ParallelTransition pt = new ParallelTransition(tt, st, ft);
            pt.setOnFinished(e -> { baseStack.getChildren().remove(fly); knownCardsOnBoard.remove(cardID); refreshAll(model); });
            pt.play();
        });
    }

    public void setPlayerOffline(String n) { if(!offlinePlayers.contains(n)) offlinePlayers.add(n); refreshAll(model); }
    public void setPlayerOnline(String n) { offlinePlayers.remove(n); refreshAll(model); }
    public void showNewEraAnimation(List<String> u, List<String> l) { if (currentEra < 3) currentEra++; refreshAll(model); }
    public void setupInitialBoard(List<String> t, Map<String, Integer> f, BoardSnapshot b) { refreshAll(model); }
    public void updatePhase(PhaseType p, String c, List<String> r) { refreshAll(model); }
    public void highlightCurrentPlayer(String n) { refreshAll(model); }
    public void updateTurnOrder(List<String> t) { refreshAll(model); }
    public void animateTotemPlacement(String n, char t) { refreshAll(model); }
    public void animateTotemReturn(String n, int p) { refreshAll(model); }
    public void updateOfferTiles(List<OfferTileInfo> o) { refreshAll(model); }
    public void updateBoardCards(List<String> u, List<String> l, int d) { refreshAll(model); }
    public void updatePlayerLimits(String n, int u, int l) { refreshAll(model); }
    public void updatePlayerResource(String n, ResourceType r, int v) { refreshAll(model); }
}