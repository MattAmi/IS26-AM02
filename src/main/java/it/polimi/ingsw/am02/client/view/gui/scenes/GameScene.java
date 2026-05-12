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

    private final List<String> selected = new ArrayList<>();
    private final List<String> offlinePlayers = new ArrayList<>();
    private final List<String> knownCardsOnBoard = new ArrayList<>();

    public Region buildNode(GuiController controller) {
        this.controller = controller;
        this.tribalFont = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 14);
        if (tribalFont == null) tribalFont = Font.font("System", 14);

        baseStack = new StackPane();
        baseStack.setMinWidth(1280);
        baseStack.setMinHeight(800);

        ImageView background = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/mesos_box.png"));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(baseStack.widthProperty());
        background.fitHeightProperty().bind(baseStack.heightProperty());
        background.setOpacity(0.4);

        root = new BorderPane();

        HBox topBanner = new HBox();
        topBanner.setPadding(new Insets(10));
        topBanner.setStyle("-fx-background-color: #A31D1D;");
        topBanner.setAlignment(Pos.CENTER);

        statusFlow = new TextFlow();
        statusFlow.setTextAlignment(TextAlignment.CENTER);
        topBanner.getChildren().add(statusFlow);
        root.setTop(topBanner);

        rightSidebar = new VBox(15);
        rightSidebar.setPadding(new Insets(20));
        rightSidebar.setPrefWidth(240);

        rightSidebar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #f4e3c5, #e6c894, #d6b170); " +
                        "-fx-effect: innershadow(three-pass-box, rgba(92,58,33,0.6), 25, 0, 0, 0); " +
                        "-fx-border-color: #3e2a1d; " +
                        "-fx-border-width: 0 0 0 5;"
        );
        root.setRight(rightSidebar);

        mainBoardArea = new VBox(25);
        mainBoardArea.setPadding(new Insets(20));
        mainBoardArea.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(mainBoardArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scrollPane);

        VBox bottomLayout = new VBox(0);
        VBox handArea = new VBox(5);
        handArea.setPadding(new Insets(10));
        handArea.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-border-color: #F2D5A3; -fx-border-width: 1 0 1 0;");

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

        HBox bottomButtons = new HBox(20);
        bottomButtons.setPadding(new Insets(15));
        bottomButtons.setAlignment(Pos.CENTER);
        bottomButtons.setStyle("-fx-background-color: #1a1a1a;");

        Button confirmBtn = new Button("CONFIRM PICK");
        confirmBtn.setFont(Font.font(tribalFont.getFamily(), 16));
        confirmBtn.setStyle("-fx-base: #5C6B32; -fx-text-fill: white; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> { controller.resolveActions(new ArrayList<>(selected)); selected.clear(); });

        Button endTurnBtn = new Button("END TURN");
        endTurnBtn.setFont(Font.font(tribalFont.getFamily(), 16));
        endTurnBtn.setStyle("-fx-base: #8B2635; -fx-text-fill: white; -fx-cursor: hand;");
        endTurnBtn.setOnAction(e -> controller.moveTotem('T'));

        Button summaryBtn = new Button("SUMMARY");
        summaryBtn.setFont(Font.font(tribalFont.getFamily(), 16));
        summaryBtn.setStyle("-fx-base: #3e2a1d; -fx-text-fill: #F2D5A3; -fx-cursor: hand;");
        summaryBtn.setOnAction(e -> controller.showSummaryCard());

        bottomButtons.getChildren().addAll(confirmBtn, endTurnBtn, summaryBtn);
        bottomLayout.getChildren().addAll(handArea, bottomButtons);
        root.setBottom(bottomLayout);

        baseStack.getChildren().addAll(background, root);
        return baseStack;
    }

    public void refreshAll(GameModel model) {
        if (model == null) return;
        this.model = model;
        if (viewedPlayerHand == null) viewedPlayerHand = model.getMyNickname();

        Platform.runLater(() -> {
            updateStatusBanner();
            updateSidebar();
            updateMainBoard();
            updateHandDisplay();
        });
    }

    private void updateStatusBanner() {
        statusFlow.getChildren().clear();
        String activePlayer = model.getCurrentPlayer() != null ? model.getCurrentPlayer() : "...";

        Text t1 = new Text("PLAYER "); t1.setFill(Color.WHITE); t1.setFont(Font.font(tribalFont.getFamily(), 18));
        Text t2 = new Text(activePlayer.toUpperCase()); t2.setFill(Color.GOLD); t2.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 22));
        Text t3 = new Text(" IS PLAYING"); t3.setFill(Color.WHITE); t3.setFont(Font.font(tribalFont.getFamily(), 18));

        statusFlow.getChildren().addAll(t1, t2, t3);
    }

    private void updateSidebar() {
        rightSidebar.getChildren().clear();

        Label sidebarTitle = new Label("PLAYERS");
        sidebarTitle.setTextFill(Color.web("#3e2a1d"));
        sidebarTitle.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 20));
        sidebarTitle.setMaxWidth(Double.MAX_VALUE);
        sidebarTitle.setAlignment(Pos.CENTER);
        rightSidebar.getChildren().add(sidebarTitle);

        List<String> players = model.getTurnOrder();
        if (players == null || players.isEmpty() || (players.size() > 0 && players.get(0).matches("\\d+"))) {
            if (model.getFoodByPlayer() != null) {
                players = new ArrayList<>(model.getFoodByPlayer().keySet());
            }
        }

        for (String nick : players) {
            VBox pBox = new VBox(5);
            pBox.setPadding(new Insets(10));
            pBox.setCursor(Cursor.HAND);
            boolean isActive = nick.equals(model.getCurrentPlayer());
            boolean isOffline = offlinePlayers.contains(nick);

            String bgColor = isActive ? "rgba(139, 38, 53, 0.15)" : "transparent";
            String borderColor = nick.equals(viewedPlayerHand) ? "#5C6B32" : "transparent";

            pBox.setStyle("-fx-background-color: " + bgColor +
                    "; -fx-border-color: " + borderColor +
                    "; -fx-border-radius: 5; -fx-border-width: 2;");
            pBox.setOpacity(isOffline ? 0.5 : 1.0);

            Label nameL = new Label(nick.toUpperCase() + (nick.equals(model.getMyNickname()) ? " (YOU)" : "") + (isOffline ? " (OFFLINE)" : ""));
            nameL.setTextFill(isActive ? Color.web("#8B2635") : (isOffline ? Color.GRAY : Color.web("#3e2a1d")));
            nameL.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 15));

            int food = model.getFoodByPlayer() != null ? model.getFoodByPlayer().getOrDefault(nick, 0) : 0;
            int pp = model.getPpByPlayer() != null ? model.getPpByPlayer().getOrDefault(nick, 0) : 0;
            int pUp = model.getRemainingUpper() != null ? model.getRemainingUpper().getOrDefault(nick, 0) : 0;
            int pLow = model.getRemainingLower() != null ? model.getRemainingLower().getOrDefault(nick, 0) : 0;

            Label statsL = new Label("Food: " + food + " | PP: " + pp + "\nPicks: " + pUp + " Up / " + pLow + " Low");
            statsL.setTextFill(Color.web("#5C3A21"));
            statsL.setFont(Font.font(tribalFont.getFamily(), 12));

            pBox.getChildren().addAll(nameL, statsL);
            pBox.setOnMouseClicked(e -> { viewedPlayerHand = nick; refreshAll(model); });
            rightSidebar.getChildren().add(pBox);
        }
    }

    private void updateMainBoard() {
        mainBoardArea.getChildren().clear();
        mainBoardArea.getChildren().addAll(
                createCardGroup(model.getUpperRow(), true),
                createTrackArea(),
                createCardGroup(model.getLowerRow(), false)
        );
    }

    private HBox createTrackArea() {
        HBox track = new HBox(25);
        track.setAlignment(Pos.CENTER);

        int currentEra = 1;
        String eraPath = "/it.polimi.ingsw.am02.images/cards/eras/back_main_era_" + currentEra + ".png";
        ImageView currentDeckView = new ImageView(ImageLoader.getImage(eraPath));
        currentDeckView.setFitHeight(150);
        currentDeckView.setPreserveRatio(true);

        int numPlayers = model.getTurnOrder().size();
        ImageView turnTile = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/turn_order_tiles/tile_turn_" + numPlayers + "p.png"));
        turnTile.setFitHeight(150); turnTile.setPreserveRatio(true);

        HBox offerTiles = new HBox(5);
        for (OfferTileInfo t : model.getOfferTiles()) offerTiles.getChildren().add(createTileView(t));

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
            VBox cardBox = createCardBox(id, false, false);
            ((ImageView) cardBox.getChildren().get(0)).setFitHeight(160);
            handCardsBox.getChildren().add(cardBox);
        }
    }

    private HBox createCardGroup(List<String> ids, boolean isUpper) {
        HBox hb = new HBox(12); hb.setAlignment(Pos.CENTER);
        if (ids != null) {
            for (String id : ids) {
                boolean isNew = !knownCardsOnBoard.contains(id);
                if (isNew) knownCardsOnBoard.add(id);
                hb.getChildren().add(createCardBox(id, isNew, isUpper));
            }
        }
        return hb;
    }

    private VBox createCardBox(String id, boolean isNew, boolean isUpper) {
        VBox b = new VBox();
        b.setCursor(Cursor.HAND);

        ImageView iv = new ImageView();
        iv.setFitHeight(170);
        iv.setPreserveRatio(true);

        if (selected.contains(id)) {
            iv.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);");
        }
        b.getChildren().add(iv);

        try {
            Tooltip tooltip = new Tooltip(CardCatalog.getInstance().format(id));
            tooltip.setWrapText(true);
            tooltip.setPrefWidth(250);
            tooltip.setShowDelay(Duration.seconds(1));
            tooltip.setShowDuration(Duration.seconds(10));
            Tooltip.install(b, tooltip);
        } catch (Exception ignored) { }

        b.setOnMouseEntered(e -> {
            iv.setTranslateY(-10);
            iv.setScaleX(1.08);
            iv.setScaleY(1.08);
            if (!selected.contains(id)) {
                iv.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.7), 15, 0.4, 0, 0);");
            }
        });

        b.setOnMouseExited(e -> {
            iv.setTranslateY(0);
            iv.setScaleX(1.0);
            iv.setScaleY(1.0);
            if (!selected.contains(id)) {
                iv.setStyle("");
            } else {
                iv.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 15, 0.6, 0, 0);");
            }
        });

        b.setOnMouseClicked(e -> {
            if(selected.contains(id)) selected.remove(id); else selected.add(id);
            refreshAll(model);
        });

        if (isNew) {
            int currentEra = 1;
            iv.setImage(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/eras/back_main_era_" + currentEra + ".png"));

            double startX = -400;
            double startY = isUpper ? 250 : -250;

            TranslateTransition move = new TranslateTransition(Duration.millis(600), iv);
            move.setFromX(startX); move.setFromY(startY);
            move.setToX(0); move.setToY(0);

            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), iv);
            scaleOut.setFromX(1.0);
            scaleOut.setToX(0.0);

            scaleOut.setOnFinished(e -> {
                iv.setImage(ImageLoader.getImage(getCardPath(id)));
                ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), iv);
                scaleIn.setFromX(0.0);
                scaleIn.setToX(1.0);
                scaleIn.play();
            });

            ParallelTransition pt = new ParallelTransition(move, scaleOut);
            pt.setDelay(Duration.millis(Math.random() * 400));
            pt.play();

        } else {
            iv.setImage(ImageLoader.getImage(getCardPath(id)));
        }

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

        st.setOnMouseEntered(e -> { st.setScaleX(1.05); st.setScaleY(1.05); });
        st.setOnMouseExited(e -> { st.setScaleX(1.0); st.setScaleY(1.0); });

        st.setOnMouseClicked(e -> controller.moveTotem(t.tileID()));
        return st;
    }

    public void setPlayerOffline(String n) { if(!offlinePlayers.contains(n)) offlinePlayers.add(n); refreshAll(model); }
    public void setPlayerOnline(String n) { offlinePlayers.remove(n); refreshAll(model); }

    public void animateCardTaken(String nickname, String cardID, CardType type, RowPosition source) {
        Platform.runLater(() -> {
            ImageView fly = new ImageView(ImageLoader.getImage(getCardPath(cardID)));
            fly.setFitHeight(170); fly.setPreserveRatio(true);

            fly.setTranslateY(source == RowPosition.UPPER ? -100 : 100);
            baseStack.getChildren().add(fly);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(1), fly);

            if (nickname.equals(model.getMyNickname())) {
                tt.setToY(350);
                tt.setToX(-150);
            } else {
                tt.setToY(-200);
                tt.setToX(500);
            }

            ScaleTransition st = new ScaleTransition(Duration.seconds(1), fly);
            st.setToX(0.1); st.setToY(0.1);

            FadeTransition ft = new FadeTransition(Duration.seconds(1), fly);
            ft.setToValue(0);

            ParallelTransition pt = new ParallelTransition(tt, st, ft);
            pt.setOnFinished(e -> {
                baseStack.getChildren().remove(fly);
                knownCardsOnBoard.remove(cardID);
                refreshAll(model);
            });
            pt.play();
        });
    }

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
}