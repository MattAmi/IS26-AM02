package it.polimi.ingsw.am02.client.view.gui.scenes;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.view.CardCatalog;
import it.polimi.ingsw.am02.client.view.gui.GuiController;
import it.polimi.ingsw.am02.client.view.gui.ImageLoader;
import it.polimi.ingsw.am02.client.view.gui.components.*;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameScene {
    private GuiController controller;
    private GameModel model;
    private StackPane baseStack;
    private BorderPane root;
    private HBox statusBox; // Modificato da TextFlow a HBox per l'allineamento perfetto
    private VBox rightSidebar;
    private VBox mainBoardArea;
    private HBox handCardsBox;
    private Label handTitle;
    private String viewedPlayerHand;
    private Font tribalFont;
    private ImageView currentDeckView;
    private Label gameIdLabel;

    private final List<String> selected = new ArrayList<>();
    private final List<String> offlinePlayers = new ArrayList<>();
    private final List<String> knownCardsOnBoard = new ArrayList<>();
    private final List<DealTask> pendingDeals = new ArrayList<>();

    private int currentEra = 1;

    private static class DealTask {
        String id; ImageView target;
        DealTask(String id, ImageView target) { this.id = id; this.target = target; }
    }

    public Region buildNode(GuiController controller, Runnable onShowMenu, Runnable onShowSummary) {
        this.controller = controller;
        this.tribalFont = Font.loadFont(getClass().getResourceAsStream("/it.polimi.ingsw.am02.fonts/intro.ttf"), 14);
        if (tribalFont == null) tribalFont = Font.font("System", 14);

        baseStack = new StackPane();
        baseStack.setMinWidth(1280); baseStack.setMinHeight(800);
        baseStack.setStyle("-fx-background-color: #0a0a0a;");

        root = new BorderPane();

        // --- TOP BANNER ---
        StackPane topBanner = new StackPane();
        topBanner.setPadding(new Insets(10, 20, 10, 20));
        topBanner.setStyle("-fx-background-color: #A31D1D;");

        HBox idBox = new HBox(10);
        idBox.setAlignment(Pos.CENTER_LEFT);

        gameIdLabel = new Label("ID: ---");
        gameIdLabel.setTextFill(Color.WHITE);
        gameIdLabel.setFont(Font.font(tribalFont.getFamily(), 14));

        // --- NUOVO BOTTONE COPIA + TOAST ---
        Button copyBtn = new Button("COPY ID");
        String btnIdle = "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 4 12; -fx-font-weight: bold;";
        String btnHover = "-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 4 12; -fx-font-weight: bold;";
        copyBtn.setStyle(btnIdle);
        if (tribalFont != null) copyBtn.setFont(Font.font(tribalFont.getFamily(), 12));

        copyBtn.setOnMouseEntered(e -> copyBtn.setStyle(btnHover));
        copyBtn.setOnMouseExited(e -> copyBtn.setStyle(btnIdle));

        Label toastLabel = new Label("COPIED!");
        toastLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 15; -fx-font-weight: bold;");
        if (tribalFont != null) toastLabel.setFont(Font.font(tribalFont.getFamily(), 10));
        toastLabel.setOpacity(0); // Invisibile di default

        copyBtn.setOnAction(e -> {
            if (model != null && model.getGameId() != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(model.getGameId());
                clipboard.setContent(content);

                // Animazione di comparsa, piccolo salto in su, e scomparsa
                toastLabel.setTranslateY(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastLabel);
                fadeIn.setFromValue(0); fadeIn.setToValue(1);

                TranslateTransition moveUp = new TranslateTransition(Duration.millis(200), toastLabel);
                moveUp.setByY(-5);

                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), toastLabel);
                fadeOut.setDelay(Duration.seconds(1.5)); // Resta visibile 1.5 secondi
                fadeOut.setFromValue(1); fadeOut.setToValue(0);

                ParallelTransition pt = new ParallelTransition(fadeIn, moveUp);
                pt.setOnFinished(ev -> fadeOut.play());
                pt.play();
            }
        });

        idBox.getChildren().addAll(gameIdLabel, copyBtn, toastLabel);
        StackPane.setAlignment(idBox, Pos.CENTER_LEFT);

        Button burgerMenuBtn = new Button("☰");
        burgerMenuBtn.setFont(Font.font(tribalFont.getFamily(), 24));
        burgerMenuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        burgerMenuBtn.setOnAction(e -> onShowMenu.run());
        StackPane.setAlignment(burgerMenuBtn, Pos.CENTER_RIGHT);

        // --- HBOX CENTRALE (Allineamento Perfetto) ---
        statusBox = new HBox(15);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setMouseTransparent(true);

        topBanner.getChildren().addAll(statusBox, idBox, burgerMenuBtn);
        root.setTop(topBanner);

        // --- RIGHT SIDEBAR ---
        rightSidebar = new VBox(15);
        rightSidebar.setPadding(new Insets(20)); rightSidebar.setPrefWidth(260);
        rightSidebar.setStyle("-fx-background-color: #1a0f07; -fx-border-color: #3e2a1d; -fx-border-width: 0 0 0 4;");
        root.setRight(rightSidebar);

        // --- CENTER AREA (Board + Hand) ---
        StackPane centerLayout = new StackPane();
        String bgPath = getClass().getResource("/it.polimi.ingsw.am02.images/mesos_box.png").toExternalForm();
        centerLayout.setStyle("-fx-background-image: url('" + bgPath + "'); -fx-background-size: 130%; -fx-background-position: center;");
        Region darkOverlay = new Region(); darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");

        mainBoardArea = new VBox(30);
        mainBoardArea.setPadding(new Insets(20, 20, 250, 20));
        mainBoardArea.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(mainBoardArea);
        scrollPane.setFitToWidth(true); scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox bottomLayout = new VBox(0);
        bottomLayout.setMaxHeight(Region.USE_PREF_SIZE); bottomLayout.setAlignment(Pos.BOTTOM_CENTER);

        VBox handArea = new VBox(5);
        handArea.setPadding(new Insets(10, 10, 30, 10));
        handArea.setStyle("-fx-background-color: rgba(26, 15, 7, 0.90); -fx-border-color: #F2D5A3; -fx-border-width: 1 0 1 0;");

        handTitle = new Label("TRIBE");
        handTitle.setTextFill(Color.web("#F2D5A3")); handTitle.setFont(Font.font(tribalFont.getFamily(), 14));

        handCardsBox = new HBox(15);
        handCardsBox.setAlignment(Pos.TOP_CENTER);
        handCardsBox.setMinHeight(220);

        ScrollPane handScroll = new ScrollPane(handCardsBox);
        handScroll.setFitToHeight(true); handScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        handScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        handArea.getChildren().addAll(handTitle, handScroll);

        HBox bottomButtons = new HBox(25);
        bottomButtons.setPadding(new Insets(15)); bottomButtons.setAlignment(Pos.CENTER);
        bottomButtons.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        String idleStyle = "-fx-background-color: #2c1a0e; -fx-text-fill: white; -fx-cursor: hand; -fx-font-family: \"" + tribalFont.getFamily() + "\"; -fx-font-weight: bold; -fx-border-color: #4A3B32; -fx-border-radius: 3; -fx-border-width: 1;";
        String hoverStyle = "-fx-background-color: #4e3219; -fx-text-fill: #F2D5A3; -fx-cursor: hand; -fx-font-family: \"" + tribalFont.getFamily() + "\"; -fx-font-weight: bold; -fx-border-color: #F2D5A3; -fx-border-radius: 3; -fx-border-width: 1;";

        Button confirmBtn = new Button("CONFIRM PICK");
        confirmBtn.setStyle(idleStyle);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(hoverStyle));
        confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(idleStyle));
        confirmBtn.setOnAction(e -> { controller.resolveActions(new ArrayList<>(selected)); selected.clear(); });

        Button endTurnBtn = new Button("END TURN");
        endTurnBtn.setStyle(idleStyle);
        endTurnBtn.setOnMouseEntered(e -> endTurnBtn.setStyle(hoverStyle));
        endTurnBtn.setOnMouseExited(e -> endTurnBtn.setStyle(idleStyle));
        endTurnBtn.setOnAction(e -> controller.moveTotem('T'));

        Button summaryBtn = new Button("SUMMARY");
        summaryBtn.setStyle(idleStyle);
        summaryBtn.setOnMouseEntered(e -> summaryBtn.setStyle(hoverStyle));
        summaryBtn.setOnMouseExited(e -> summaryBtn.setStyle(idleStyle));
        summaryBtn.setOnAction(e -> onShowSummary.run());

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

        // FIX ERA: Se il tuo model conosce l'era attuale, la sincronizziamo.
        // Togli il commento dalla riga sotto se il model ha un metodo getEra() o getRound()
        // if (model.getEra() > 0) this.currentEra = model.getEra();

        Platform.runLater(() -> {
            gameIdLabel.setText("ID: " + (model.getGameId() != null ? model.getGameId() : "---"));
            updateStatusBanner();
            updateSidebar();
            updateMainBoard();
            updateHandDisplay();

            root.applyCss();
            root.layout();

            if (!pendingDeals.isEmpty()) {
                for (DealTask task : pendingDeals) animateDeal(task.id, task.target);
                pendingDeals.clear();
            }
        });
    }

    private void updateStatusBanner() {
        statusBox.getChildren().clear();
        String activePlayer = model.getCurrentPlayer() != null ? model.getCurrentPlayer().toUpperCase() : "...";
        String currentPhase = model.getCurrentPhase() != null ? model.getCurrentPhase().toString().replace("_", " ") : "WAITING";

        // Usiamo Label dentro HBox per un allineamento verticale millimetrico
        Label eraTxt = new Label("ERA " + currentEra + "  |");
        eraTxt.setTextFill(Color.WHITE);
        eraTxt.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 18));

        Label phaseTxt = new Label(currentPhase + "  |");
        phaseTxt.setTextFill(Color.LIGHTGRAY);
        phaseTxt.setFont(Font.font(tribalFont.getFamily(), 16));

        Label p1 = new Label("PLAYER");
        p1.setTextFill(Color.WHITE);
        p1.setFont(Font.font(tribalFont.getFamily(), 16));

        Label p2 = new Label(activePlayer);
        p2.setTextFill(Color.GOLD);
        p2.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 18));

        statusBox.getChildren().addAll(eraTxt, phaseTxt, p1, p2);
    }

    private void updateSidebar() {
        rightSidebar.getChildren().clear();
        Label sidebarTitle = new Label("PLAYERS");
        sidebarTitle.setTextFill(Color.WHITE); sidebarTitle.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 20));
        sidebarTitle.setMaxWidth(Double.MAX_VALUE); sidebarTitle.setAlignment(Pos.CENTER);
        rightSidebar.getChildren().add(sidebarTitle);

        List<String> players = model.getTurnOrder();
        if (players == null || players.isEmpty() || (!players.isEmpty() && players.get(0).matches("\\d+"))) {
            if (model.getFoodByPlayer() != null) players = new ArrayList<>(model.getFoodByPlayer().keySet());
        }

        if (players != null) {
            for (String nick : players) {
                boolean isActive = nick.equals(model.getCurrentPlayer());
                boolean isOffline = offlinePlayers.contains(nick);
                boolean isViewed = nick.equals(viewedPlayerHand);
                int food = model.getFoodByPlayer().getOrDefault(nick, 0);
                int pp = model.getPpByPlayer().getOrDefault(nick, 0);

                PlayerSidebarItem pBox = new PlayerSidebarItem(
                        nick, nick.equals(model.getMyNickname()), isActive, isOffline, isViewed,
                        food, pp, model.getTotem(nick), tribalFont,
                        () -> { viewedPlayerHand = nick; refreshAll(model); }
                );

                rightSidebar.getChildren().add(pBox);
            }
        }
    }

    private void updateMainBoard() {
        mainBoardArea.getChildren().clear();
        HBox upperBand = new HBox(40); upperBand.setAlignment(Pos.CENTER);
        upperBand.getChildren().addAll(createCardGroup(model.getUpperRowBuildings()), createCardGroup(model.getUpperRow()));
        HBox lowerBand = new HBox(40); lowerBand.setAlignment(Pos.CENTER);
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
        TurnOrderCaveView turnOrderCave = new TurnOrderCaveView(numPlayers, model.getTurnOrderSlots(), model);

        HBox offerTiles = new HBox(5);
        if (model.getOfferTiles() != null) {
            for (OfferTileInfo t : model.getOfferTiles()) {
                Totem occupantTotem = t.occupantNickname() != null ? model.getTotem(t.occupantNickname()) : null;
                OfferTileView tileView = new OfferTileView(t, occupantTotem, controller);
                offerTiles.getChildren().add(tileView);
            }
        }

        track.getChildren().addAll(currentDeckView, turnOrderCave, offerTiles);
        return track;
    }

    private String extractCategoryFromId(String cardId) {
        if (cardId.startsWith("B_")) return "BUILDING";

        String cardInfo = CardCatalog.getInstance().format(cardId).toUpperCase();

        if (cardInfo.contains("INVENTOR")) return "INVENTOR";
        if (cardInfo.contains("BUILDER")) return "BUILDER";
        if (cardInfo.contains("GATHERER")) return "GATHERER";
        if (cardInfo.contains("ARTIST")) return "ARTIST";
        if (cardInfo.contains("SHAMAN")) return "SHAMAN";
        if (cardInfo.contains("HUNTER")) return "HUNTER";

        System.err.println("ATTENZIONE: Tipo carta non riconosciuto per: " + cardId + ". Stringa catalogo: " + cardInfo);
        return "UNKNOWN";
    }

    private void updateHandDisplay() {
        handCardsBox.getChildren().clear();
        handTitle.setText("TRIBE OF: " + viewedPlayerHand.toUpperCase());

        List<String> chars = model.getCharactersByPlayer().getOrDefault(viewedPlayerHand, List.of());
        List<String> buildings = model.getBuildingsByPlayer().getOrDefault(viewedPlayerHand, List.of());

        List<String> allCards = new ArrayList<>(chars);
        allCards.addAll(buildings);

        String[] columnOrder = {"INVENTOR", "BUILDER", "GATHERER", "ARTIST", "SHAMAN", "HUNTER", "BUILDING"};

        for (String category : columnOrder) {
            List<String> cardsInCategory = allCards.stream()
                    .filter(id -> extractCategoryFromId(id).equals(category))
                    .collect(Collectors.toList());

            handCardsBox.getChildren().add(createCascadingStack(category, cardsInCategory));
        }
    }

    private StackPane createCascadingStack(String category, List<String> cardIds) {
        StackPane stack = new StackPane();
        stack.setAlignment(Pos.TOP_CENTER);
        int offsetPerCard = 25;

        if (cardIds.isEmpty()) {
            stack.setPrefWidth(90);
            return stack;
        }

        stack.setPadding(new Insets(20, 0, Math.max(0, cardIds.size() - 1) * offsetPerCard, 0));

        for (int i = 0; i < cardIds.size(); i++) {
            String id = cardIds.get(i);
            // Click sulla carta in mano -> Lancia lo Zoom per vedere meglio
            GameCardView cardView = new GameCardView(id, false, 130, () -> showZoomedCard(id));
            cardView.setTranslateY(i * offsetPerCard);
            stack.getChildren().add(cardView);
        }

        int totaleStelle = 0;
        int totaleScontoEdifici = 0;
        int totalePP = 0;
        int totaleScontoCibo = 0;
        long simboliInventoreUnici = 0;

        if (category.equals("SHAMAN")) {
            totaleStelle = cardIds.stream().mapToInt(this::extractShamanStars).sum();
        } else if (category.equals("BUILDER")) {
            totaleScontoEdifici = cardIds.stream().mapToInt(this::extractBuilderDiscount).sum();
            totalePP = cardIds.stream().mapToInt(this::extractPrestigePoints).sum();
        } else if (category.equals("GATHERER")) {
            totaleScontoCibo = cardIds.stream().mapToInt(this::extractGathererDiscount).sum();
        } else if (category.equals("INVENTOR")) {
            simboliInventoreUnici = cardIds.stream().map(this::extractInventorSymbol).filter(s -> !s.isEmpty()).distinct().count();
        }

        String countBadge = "(x" + cardIds.size() + ")";
        String infoText = "";

        if (category.equals("SHAMAN") && totaleStelle > 0) infoText = countBadge + " | " + totaleStelle + " ⭐";
        else if (category.equals("BUILDER")) infoText = countBadge + " | -" + totaleScontoEdifici + " Cost | " + totalePP + " PP";
        else if (category.equals("GATHERER") && totaleScontoCibo > 0) infoText = countBadge + " | -" + totaleScontoCibo + " Food";
        else if (category.equals("INVENTOR") && simboliInventoreUnici > 0) infoText = countBadge + " | " + simboliInventoreUnici + " 💡";
        else infoText = category.substring(0, Math.min(3, category.length())) + " " + countBadge;

        Label infoBadge = new Label(infoText);
        infoBadge.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-text-fill: gold; -fx-padding: 3 8; -fx-background-radius: 5; -fx-border-color: gold; -fx-border-radius: 5;");
        if (tribalFont != null) infoBadge.setFont(Font.font(tribalFont.getFamily(), 10));
        infoBadge.setTranslateY(-15);
        StackPane.setAlignment(infoBadge, Pos.TOP_CENTER);
        stack.getChildren().add(infoBadge);

        return stack;
    }

    // --- PARSERS ---

    private int extractPrestigePoints(String cardId) {
        String desc = CardCatalog.getInstance().format(cardId);
        String target = "PP:";
        int index = desc.indexOf(target);
        if (index != -1) {
            int end = desc.indexOf(" ", index + target.length());
            if (end == -1) end = desc.length();
            try { return Integer.parseInt(desc.substring(index + target.length(), end).trim()); }
            catch (Exception ignored) {}
        }
        return 0;
    }

    private String extractInventorSymbol(String cardId) {
        String desc = CardCatalog.getInstance().format(cardId);
        int start = desc.indexOf(" (");
        if (start != -1) {
            int end = desc.indexOf(")", start);
            if (end != -1) {
                return desc.substring(start + 2, end).trim();
            }
        }
        return "";
    }

    private int extractShamanStars(String cardId) {
        String desc = CardCatalog.getInstance().getFullDescription(cardId);
        String target = "Shaman Stars: ";
        int index = desc.indexOf(target);
        if (index != -1) {
            int end = desc.indexOf("\n", index + target.length());
            if (end != -1) {
                try { return Integer.parseInt(desc.substring(index + target.length(), end).trim()); }
                catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private int extractBuilderDiscount(String cardId) {
        String desc = CardCatalog.getInstance().getFullDescription(cardId);
        String target = "Building Discount: -";
        int index = desc.indexOf(target);
        if (index != -1) {
            int end = desc.indexOf(" Food", index + target.length());
            if (end != -1) {
                try { return Integer.parseInt(desc.substring(index + target.length(), end).trim()); }
                catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private int extractGathererDiscount(String cardId) {
        String desc = CardCatalog.getInstance().getFullDescription(cardId);
        String target = "Food Discount: -";
        int index = desc.indexOf(target);
        if (index != -1) {
            int end = desc.indexOf(" Food", index + target.length());
            if (end != -1) {
                try { return Integer.parseInt(desc.substring(index + target.length(), end).trim()); }
                catch (Exception ignored) {}
            }
        }
        return 0;
    }

    // ----------------------------------------------------------------------------

    private void toggleCardSelection(String id) {
        if (model != null && !model.getMyNickname().equals(model.getCurrentPlayer())) {
            return;
        }

        if(selected.contains(id)) selected.remove(id);
        else selected.add(id);
        refreshAll(model);
    }

    private HBox createCardGroup(List<String> ids) {
        HBox hb = new HBox(12); hb.setAlignment(Pos.CENTER);
        if (ids != null) {
            for (String id : ids) {
                boolean isNew = !knownCardsOnBoard.contains(id);
                if (isNew) knownCardsOnBoard.add(id);

                GameCardView cardView = new GameCardView(id, selected.contains(id), 170, () -> toggleCardSelection(id));

                if (isNew) {
                    cardView.getCardImageView().setOpacity(0.0);
                    pendingDeals.add(new DealTask(id, cardView.getCardImageView()));
                }
                hb.getChildren().add(cardView);
            }
        }
        return hb;
    }

    private void animateDeal(String id, ImageView target) {
        if (currentDeckView == null || currentDeckView.getScene() == null || target.getScene() == null) {
            target.setOpacity(1.0); return;
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

        ScaleTransition st1 = new ScaleTransition(Duration.millis(350), fly); st1.setToX(0);
        st1.setOnFinished(e -> {
            String sub = id.startsWith("C_") ? "characters/" : id.startsWith("B_") ? "buildings/" : "events/";
            fly.setImage(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/" + sub + id + ".png"));
            ScaleTransition st2 = new ScaleTransition(Duration.millis(350), fly);
            st2.setToX(1.0); st2.play();
        });

        ParallelTransition pt = new ParallelTransition(tt, st1);
        pt.setOnFinished(e -> { baseStack.getChildren().remove(fly); target.setOpacity(1.0); });
        pt.play();
    }

    public void animateCardTaken(String nickname, String cardID, CardType type, RowPosition source) {
        Platform.runLater(() -> {
            String sub = cardID.startsWith("C_") ? "characters/" : cardID.startsWith("B_") ? "buildings/" : "events/";
            ImageView fly = new ImageView(ImageLoader.getImage("/it.polimi.ingsw.am02.images/cards/" + sub + cardID + ".png"));
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

    // --- FUNZIONE ZOOM: Mostra la carta in grande ---
    private void showZoomedCard(String cardId) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);"); // Sfondo scuro

        String sub = cardId.startsWith("C_") ? "characters/" : cardId.startsWith("B_") ? "buildings/" : "events/";
        String path = "/it.polimi.ingsw.am02.images/cards/" + sub + cardId + ".png";

        ImageView bigCard = new ImageView(ImageLoader.getImage(path));
        bigCard.setFitHeight(550);
        bigCard.setPreserveRatio(true);
        bigCard.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 40, 0.4, 0, 0);");

        bigCard.setScaleX(0.5);
        bigCard.setScaleY(0.5);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), bigCard);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();

        overlay.setOnMouseClicked(e -> baseStack.getChildren().remove(overlay));

        overlay.getChildren().add(bigCard);
        baseStack.getChildren().add(overlay);
    }

    public void showNewEraAnimation(List<String> u, List<String> l) {
        if (currentEra < 3) currentEra++; // Aumenta l'era!
        refreshAll(model); // Ridisegna tutto con la nuova era
    }
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