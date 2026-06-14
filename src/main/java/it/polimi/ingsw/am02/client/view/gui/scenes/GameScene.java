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
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;
import it.polimi.ingsw.am02.common.enumerations.Era;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * The main game scene of the GUI.
 * This class handles the rendering of the game board, player hands, sidebar,
 * and animations for various game events.
 */
public class GameScene {
    private GuiController controller;
    private GameModel model;
    private StackPane baseStack;
    private StackPane modalLayer;
    private BorderPane root;
    private HBox statusBox;
    private VBox rightSidebar;
    private VBox mainBoardArea;
    private HBox handCardsBox;
    private Label handTitle;
    private String viewedPlayerHand;
    private Font tribalFont;
    private ImageView currentDeckView;
    private Label gameIdLabel;

    private VBox notificationPanel;
    private VBox logContainer;

    private VBox forfeitBanner;

    private final List<String> selected = new ArrayList<>();
    private final List<String> offlinePlayers = new ArrayList<>();
    private final List<String> knownCardsOnBoard = new ArrayList<>();

    private final Queue<Runnable> animationQueue = new LinkedList<>();
    private boolean isAnimating = false;

    // Catch-up guard: while the reconnection event history is
    // being fast-forwarded, all interactive controls are locked so the player cannot
    // fire a command against a stale frame of the replay.
    private boolean isReplaying = false;
    private PauseTransition replayWatchdog;
    private Button confirmBtn;
    private Button endTurnBtn;
    private SceneState lastState;

    /**
     * Represents a task for dealing a card.
     */
    private static class DealTask {
        String id; ImageView target;
        DealTask(String id, ImageView target) { this.id = id; this.target = target; }
    }

    /**
     * Inner class representing a snapshot of the game state for rendering and animations.
     */
    private static class SceneState {
        String gameId; String currentPlayer; PhaseType currentPhase; Era era; String myNickname;
        List<String> turnOrder; Map<String, Integer> foodByPlayer; Map<String, Integer> ppByPlayer;
        Map<String, Totem> totems; List<String> upperRow; List<String> upperRowBuildings;
        List<String> lowerRow; List<String> lowerRowBuildings; List<OfferTileInfo> offerTiles;
        List<TurnOrderSlotInfo> turnOrderSlots;
        Map<String, Integer> remainingUpper; Map<String, Integer> remainingLower;
        Map<String, List<String>> charactersByPlayer; Map<String, List<String>> buildingsByPlayer;

        public SceneState(GameModel m) {
            this.gameId = m.getGameId(); this.currentPlayer = m.getCurrentPlayer();
            this.currentPhase = m.getCurrentPhase();
            this.era = m.getCurrentEra() != null ? m.getCurrentEra() : Era.I;
            this.myNickname = m.getMyNickname();
            this.turnOrder = m.getTurnOrder() != null ? new ArrayList<>(m.getTurnOrder()) : new ArrayList<>();
            this.foodByPlayer = m.getFoodByPlayer() != null ? new HashMap<>(m.getFoodByPlayer()) : new HashMap<>();
            this.ppByPlayer = m.getPpByPlayer() != null ? new HashMap<>(m.getPpByPlayer()) : new HashMap<>();
            this.totems = new HashMap<>();
            if (m.getTurnOrder() != null) {
                for (String p : m.getTurnOrder()) {
                    Totem t = m.getTotem(p);
                    if (t != null) this.totems.put(p, t);
                }
            }
            this.upperRow = m.getUpperRow() != null ? new ArrayList<>(m.getUpperRow()) : new ArrayList<>();
            this.upperRowBuildings = m.getUpperRowBuildings() != null ? new ArrayList<>(m.getUpperRowBuildings()) : new ArrayList<>();
            this.lowerRow = m.getLowerRow() != null ? new ArrayList<>(m.getLowerRow()) : new ArrayList<>();
            this.lowerRowBuildings = m.getLowerRowBuildings() != null ? new ArrayList<>(m.getLowerRowBuildings()) : new ArrayList<>();
            this.offerTiles = m.getOfferTiles() != null ? new ArrayList<>(m.getOfferTiles()) : new ArrayList<>();
            this.turnOrderSlots = m.getTurnOrderSlots() != null ? new ArrayList<>(m.getTurnOrderSlots()) : new ArrayList<>();
            this.remainingUpper = m.getRemainingUpper() != null ? new HashMap<>(m.getRemainingUpper()) : new HashMap<>();
            this.remainingLower = m.getRemainingLower() != null ? new HashMap<>(m.getRemainingLower()) : new HashMap<>();
            this.charactersByPlayer = new HashMap<>();
            if (m.getCharactersByPlayer() != null) m.getCharactersByPlayer().forEach((k, v) -> this.charactersByPlayer.put(k, new ArrayList<>(v)));
            this.buildingsByPlayer = new HashMap<>();
            if (m.getBuildingsByPlayer() != null) m.getBuildingsByPlayer().forEach((k, v) -> this.buildingsByPlayer.put(k, new ArrayList<>(v)));
        }
    }

    /**
     * Plays the next animation in the queue.
     */
    private void playNextAnimation() {
        if (animationQueue.isEmpty()) {
            isAnimating = false;
            return;
        }
        isAnimating = true;
        Runnable next = animationQueue.poll();
        next.run();
    }

    /**
     * Builds the game scene node.
     *
     * @param controller    The GUI controller.
     * @param onShowMenu    Callback to show the in-game menu.
     * @param onShowSummary Callback to show the summary card.
     * @return The constructed Region representing the scene.
     */
    public Region buildNode(GuiController controller, Runnable onShowMenu, Runnable onShowSummary) {
        this.controller = controller;
        this.tribalFont = Font.loadFont(getClass().getResourceAsStream("/fonts/intro.ttf"), 14);
        if (tribalFont == null) tribalFont = Font.font("System", 14);

        baseStack = new StackPane(); baseStack.setMinWidth(1280); baseStack.setMinHeight(800);
        baseStack.setStyle("-fx-background-color: #0a0a0a;");
        root = new BorderPane();

        StackPane topBanner = new StackPane();
        topBanner.setPadding(new Insets(10, 20, 10, 20)); topBanner.setStyle("-fx-background-color: #A31D1D;");

        HBox idBox = new HBox(10); idBox.setAlignment(Pos.CENTER_LEFT);
        idBox.setPickOnBounds(false);
        gameIdLabel = new Label("ID: ---"); gameIdLabel.setTextFill(Color.WHITE); gameIdLabel.setFont(Font.font(tribalFont.getFamily(), 14));

        Button copyBtn = new Button("COPY ID");
        copyBtn.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        String fName = tribalFont != null ? tribalFont.getFamily() : "System";
        String btnIdle = "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 4 12; -fx-font-family: \"" + fName + "\"; -fx-font-size: 12;";
        String btnHover = "-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 4 12; -fx-font-family: \"" + fName + "\"; -fx-font-size: 12;";
        copyBtn.setStyle(btnIdle);
        copyBtn.setOnMouseEntered(e -> copyBtn.setStyle(btnHover)); copyBtn.setOnMouseExited(e -> copyBtn.setStyle(btnIdle));

        Label toastLabel = new Label("COPIED!");
        toastLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 15; -fx-font-weight: bold;");
        if (tribalFont != null) toastLabel.setFont(Font.font(tribalFont.getFamily(), 10));
        toastLabel.setOpacity(0);
        toastLabel.setMouseTransparent(true);

        StackPane copyContainer = new StackPane();
        copyContainer.setAlignment(Pos.CENTER);
        copyContainer.getChildren().addAll(copyBtn, toastLabel);

        copyBtn.setOnAction(e -> {
            if (model != null && model.getGameId() != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard(); ClipboardContent content = new ClipboardContent();
                content.putString(model.getGameId()); clipboard.setContent(content);

                toastLabel.setTranslateY(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastLabel); fadeIn.setFromValue(0); fadeIn.setToValue(1);
                TranslateTransition moveUp = new TranslateTransition(Duration.millis(200), toastLabel);
                moveUp.setByY(-25);
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), toastLabel); fadeOut.setDelay(Duration.seconds(1.5)); fadeOut.setFromValue(1); fadeOut.setToValue(0);
                ParallelTransition pt = new ParallelTransition(fadeIn, moveUp); pt.setOnFinished(ev -> fadeOut.play()); pt.play();
            }
        });

        idBox.getChildren().addAll(gameIdLabel, copyContainer);
        StackPane.setAlignment(idBox, Pos.CENTER_LEFT);

        HBox rightControls = new HBox(15);
        rightControls.setAlignment(Pos.CENTER_RIGHT);
        rightControls.setPickOnBounds(false);

        Button logsBtn = new Button("▼");
        logsBtn.setFont(Font.font(tribalFont.getFamily(), 8));
        logsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");

        Button burgerMenuBtn = new Button("☰"); burgerMenuBtn.setFont(Font.font(tribalFont.getFamily(), 20));
        burgerMenuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        burgerMenuBtn.setOnAction(e -> onShowMenu.run()); StackPane.setAlignment(burgerMenuBtn, Pos.CENTER_RIGHT);

        rightControls.getChildren().addAll(logsBtn, burgerMenuBtn);
        StackPane.setAlignment(rightControls, Pos.CENTER_RIGHT);

        statusBox = new HBox(15); statusBox.setAlignment(Pos.CENTER); statusBox.setMouseTransparent(true);
        topBanner.getChildren().addAll(statusBox, idBox, rightControls); root.setTop(topBanner);

        rightSidebar = new VBox(15); rightSidebar.setPadding(new Insets(20)); rightSidebar.setPrefWidth(260);
        rightSidebar.setStyle("-fx-background-color: #1a0f07; -fx-border-color: #3e2a1d; -fx-border-width: 0 0 0 4;");
        root.setRight(rightSidebar);

        StackPane centerLayout = new StackPane();
        String bgPath = getClass().getResource("/images/mesos_box.png").toExternalForm();
        centerLayout.setStyle("-fx-background-image: url('" + bgPath + "'); -fx-background-size: 130%; -fx-background-position: center;");
        Region darkOverlay = new Region(); darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");

        // Fixed bottom reservation for the overlaid hand/TRIBE strip. The strip is
        // height-bounded below (fixed tribe-card size + capped cascade spread + tight
        // paddings), so it never grows past this reservation as a tribe accumulates
        // cards. A fixed value (rather than one bound to the strip height) keeps the
        // board from scrolling, while still guaranteeing the lower row is never
        // covered, for any tribe size and 2-5 players.
        mainBoardArea = new VBox(30); mainBoardArea.setPadding(new Insets(20, 20, 255, 20)); mainBoardArea.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(mainBoardArea); scrollPane.setFitToWidth(true); scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox bottomLayout = new VBox(0); bottomLayout.setMaxHeight(Region.USE_PREF_SIZE); bottomLayout.setAlignment(Pos.BOTTOM_CENTER);

        VBox handArea = new VBox(5); handArea.setPadding(new Insets(10, 10, 12, 10));
        handArea.setStyle("-fx-background-color: rgba(26, 15, 7, 0.90); -fx-border-color: #F2D5A3; -fx-border-width: 1 0 1 0;");
        handTitle = new Label("TRIBE"); handTitle.setTextFill(Color.web("#F2D5A3")); handTitle.setFont(Font.font(tribalFont.getFamily(), 14));

        handCardsBox = new HBox(15); handCardsBox.setAlignment(Pos.TOP_CENTER); handCardsBox.setMinHeight(155);
        ScrollPane handScroll = new ScrollPane(handCardsBox); handScroll.setFitToHeight(true); handScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        handScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        handArea.getChildren().addAll(handTitle, handScroll);

        HBox bottomButtons = new HBox(25); bottomButtons.setPadding(new Insets(10)); bottomButtons.setAlignment(Pos.CENTER);
        bottomButtons.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        String idleStyle = "-fx-background-color: #2c1a0e; -fx-text-fill: white; -fx-cursor: hand; -fx-font-family: \"" + fName + "\"; -fx-border-color: #4A3B32; -fx-border-radius: 3; -fx-border-width: 1;";
        String hoverStyle = "-fx-background-color: #4e3219; -fx-text-fill: #F2D5A3; -fx-cursor: hand; -fx-font-family: \"" + fName + "\"; -fx-border-color: #F2D5A3; -fx-border-radius: 3; -fx-border-width: 1;";

        confirmBtn = new Button("CONFIRM PICK"); confirmBtn.setStyle(idleStyle);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(hoverStyle)); confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(idleStyle));
        confirmBtn.setOnAction(e -> { if (!cardActionsAllowed()) return; controller.resolveActions(new ArrayList<>(selected)); selected.clear(); });

        endTurnBtn = new Button("END TURN"); endTurnBtn.setStyle(idleStyle);
        endTurnBtn.setOnMouseEntered(e -> endTurnBtn.setStyle(hoverStyle)); endTurnBtn.setOnMouseExited(e -> endTurnBtn.setStyle(idleStyle));
        endTurnBtn.setOnAction(e -> { if (!actionsAllowed()) return; controller.moveTotem('T'); });

        Button summaryBtn = new Button("SUMMARY"); summaryBtn.setStyle(idleStyle);
        summaryBtn.setOnMouseEntered(e -> summaryBtn.setStyle(hoverStyle)); summaryBtn.setOnMouseExited(e -> summaryBtn.setStyle(idleStyle));
        summaryBtn.setOnAction(e -> onShowSummary.run());

        bottomButtons.getChildren().addAll(confirmBtn, endTurnBtn, summaryBtn);
        bottomLayout.getChildren().addAll(handArea, bottomButtons);
        StackPane.setAlignment(bottomLayout, Pos.BOTTOM_CENTER);
        centerLayout.getChildren().addAll(darkOverlay, scrollPane, bottomLayout);
        root.setCenter(centerLayout); baseStack.getChildren().add(root);

        notificationPanel = new VBox(10);
        notificationPanel.setMaxSize(380, Region.USE_PREF_SIZE);
        notificationPanel.setStyle("-fx-background-color: rgba(26, 15, 7, 0.95); -fx-border-color: #F2D5A3; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10;");
        notificationPanel.setVisible(false);

        Label logTitle = new Label("RECENT LOGS");
        logTitle.setTextFill(Color.web("#F2D5A3"));
        logTitle.setFont(Font.font(tribalFont.getFamily(), 14));

        logContainer = new VBox(5);
        notificationPanel.getChildren().addAll(logTitle, logContainer);

        StackPane.setAlignment(notificationPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(notificationPanel, new Insets(60, 20, 0, 0));
        baseStack.getChildren().add(notificationPanel);

        logsBtn.setOnAction(e -> {
            notificationPanel.setVisible(!notificationPanel.isVisible());
            logsBtn.setText(notificationPanel.isVisible() ? "▲" : "▼");
        });

        modalLayer = new StackPane();
        modalLayer.setVisible(false);
        modalLayer.setStyle("-fx-background-color: transparent;");
        baseStack.getChildren().add(modalLayer);
        return baseStack;
    }

    /**
     * Logs the resolution of an event.
     *
     * @param eventName The name of the event.
     */
    public void logEventResolved(String eventName) {
        Platform.runLater(() -> {
            addLogEntry("[EVENT] ", "Resolved: " + eventName, Color.web("#9C27B0"));
        });
    }

    /**
     * Logs a resource change for a player.
     *
     * @param nickname The nickname of the player.
     * @param resource The type of resource.
     * @param newValue The new value of the resource.
     */
    public void logResourceChanged(String nickname, ResourceType resource, int newValue) {
        Platform.runLater(() -> {
            addLogEntry("[RESOURCE] ", nickname + " now has " + newValue + " " + resource, Color.GOLD);
        });
    }

    /**
     * Logs that a player placed their totem on an offer tile.
     * Mirrors the TUI {@code [BOARD]} notification.
     *
     * @param nickname The nickname of the player.
     * @param tileID   The tile the totem was placed on.
     */
    public void logTotemPlaced(String nickname, char tileID) {
        Platform.runLater(() -> addLogEntry("[BOARD] ", nickname + " placed their totem on tile " + tileID, Color.WHITE));
    }

    /**
     * Logs that a player's totem returned to the turn-order tile.
     * Mirrors the TUI {@code [BOARD]} notification.
     *
     * @param nickname The nickname of the player.
     * @param position The zero-based turn-order slot index.
     */
    public void logTotemReturned(String nickname, int position) {
        Platform.runLater(() -> addLogEntry("[BOARD] ", nickname + " returned to turn-order slot " + (position + 1), Color.WHITE));
    }

    /**
     * Logs that a player took a card from the board.
     * Mirrors the TUI {@code [ACTION]} notification.
     *
     * @param nickname The nickname of the player.
     * @param cardID   The ID of the card taken.
     */
    public void logCardTaken(String nickname, String cardID) {
        Platform.runLater(() -> addLogEntry("[ACTION] ", nickname + " took " + CardCatalog.getInstance().format(cardID), Color.web("#4DD0E1")));
    }

    /**
     * Logs that a player gained an extra turn.
     * Mirrors the TUI {@code [EXTRA]} notification.
     *
     * @param nickname       The nickname of the player.
     * @param remainingUpper Remaining upper-row picks.
     * @param remainingLower Remaining lower-row picks.
     */
    public void logExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        Platform.runLater(() -> addLogEntry("[EXTRA] ",
                nickname + " gained an extra turn! (Up:" + remainingUpper + " Lw:" + remainingLower + ")",
                Color.web("#4CAF50")));
    }

    /**
     * Logs that a player's extra turn ended.
     * Mirrors the TUI {@code [EXTRA]} notification.
     *
     * @param nickname The nickname of the player.
     */
    public void logExtraTurnEnded(String nickname) {
        Platform.runLater(() -> addLogEntry("[EXTRA] ", nickname + "'s extra turn ended.", Color.GOLD));
    }

    /**
     * Logs the start of the auto-player timer.
     *
     * @param nickname The nickname of the disconnected player.
     */
    public void logAutoPlayerTimerStarted(String nickname, long seconds) {
        Platform.runLater(() -> {
            addLogEntry("[BOT] ", nickname + " disconnected — AutoPlayer takes over in " + seconds + "s.", Color.GOLD);
        });
    }

    /**
     * Logs the invocation of the auto-player.
     *
     * @param nickname The nickname of the player being substituted.
     */
    public void logAutoPlayerInvoked(String nickname) {
        Platform.runLater(() -> {
            addLogEntry("[BOT] ", "AutoPlayer acting for " + nickname + "...", Color.web("#9C27B0"));
        });
    }

    /**
     * Adds an entry to the log container.
     *
     * @param prefix      The prefix for the log entry.
     * @param message     The log message.
     * @param prefixColor The color for the prefix text.
     */
    private void addLogEntry(String prefix, String message, Color prefixColor) {
        Text pText = new Text(prefix);
        pText.setFill(prefixColor);
        if (tribalFont != null) pText.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 14));

        Text mText = new Text(message);
        mText.setFill(Color.WHITE);
        if (tribalFont != null) mText.setFont(Font.font(tribalFont.getFamily(), 14));

        TextFlow tf = new TextFlow(pText, mText);
        tf.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 5; -fx-background-radius: 3;");

        logContainer.getChildren().add(tf);
        if (logContainer.getChildren().size() > 10) logContainer.getChildren().remove(0);
    }

    /**
     * Refreshes the entire game scene based on the current model state.
     * Takes a snapshot of the model to ensure thread safety during rendering.
     *
     * @param model The current game model.
     */
    public void refreshAll(GameModel model) {
        if (model == null) return;
        this.model = model;
        if (viewedPlayerHand == null) viewedPlayerHand = model.getMyNickname();

        SceneState snapshot = new SceneState(model);
        this.lastState = snapshot;

        Platform.runLater(() -> {
            if (isReplaying) armReplayWatchdog();
            animationQueue.add(() -> refreshAllInternal(snapshot));
            if (!isAnimating) playNextAnimation();
        });
    }

    /**
     * Internal refresh method that updates all UI components based on a state snapshot.
     *
     * @param state The state snapshot to render.
     */
    private void refreshAllInternal(SceneState state) {
        gameIdLabel.setText("ID: " + (state.gameId != null ? state.gameId : "---"));
        updateStatusBanner(state);
        updateSidebar(state);

        List<DealTask> localDeals = new ArrayList<>();
        updateMainBoard(state, localDeals);
        updateHandDisplay(state);
        updateActionButtonsState();

        root.applyCss();
        root.layout();

        if (!localDeals.isEmpty()) {
            ParallelTransition allDeals = new ParallelTransition();
            for (DealTask task : localDeals) {
                allDeals.getChildren().add(createSingleDealAnimation(task.id, task.target, state.era.ordinal() + 1));
            }
            allDeals.setOnFinished(e -> {
                for (DealTask task : localDeals) task.target.setOpacity(1.0);
                playNextAnimation();
            });
            allDeals.play();
        } else {
            playNextAnimation();
        }
    }

    /**
     * Updates the status banner with the current era, phase, and active player.
     *
     * @param state The current state snapshot.
     */
    private void updateStatusBanner(SceneState state) {
        statusBox.getChildren().clear();
        String activePlayer = state.currentPlayer != null ? state.currentPlayer : "...";
        String currentPhase = state.currentPhase != null ? state.currentPhase.toString().replace("_", " ") : "WAITING";

        Label eraTxt = new Label("ERA " + state.era + "  |"); eraTxt.setTextFill(Color.WHITE); eraTxt.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 18));
        Label phaseTxt = new Label(currentPhase + "  |"); phaseTxt.setTextFill(Color.LIGHTGRAY); phaseTxt.setFont(Font.font(tribalFont.getFamily(), 16));
        Label p1 = new Label("PLAYER"); p1.setTextFill(Color.WHITE); p1.setFont(Font.font(tribalFont.getFamily(), 16));
        Label p2 = new Label(activePlayer); p2.setTextFill(Color.GOLD); p2.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 18));
        statusBox.getChildren().addAll(eraTxt, phaseTxt, p1, p2);
    }

    /**
     * Updates the sidebar with the list of players and their status.
     *
     * @param state The current state snapshot.
     */
    private void updateSidebar(SceneState state) {
        rightSidebar.getChildren().clear();
        Label sidebarTitle = new Label("PLAYERS"); sidebarTitle.setTextFill(Color.WHITE); sidebarTitle.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 20));
        sidebarTitle.setMaxWidth(Double.MAX_VALUE); sidebarTitle.setAlignment(Pos.CENTER);
        rightSidebar.getChildren().add(sidebarTitle);

        List<String> players = state.turnOrder;
        if (players.isEmpty() || players.get(0).matches("\\d+")) players = new ArrayList<>(state.foodByPlayer.keySet());

        // Remaining picks are only meaningful during ACTION_RESOLUTION; the model
        // keeps the last values around between turns, so outside that phase we
        // force them to 0 to match the TUI's per-phase semantics.
        boolean inActionResolution = state.currentPhase == PhaseType.ACTION_RESOLUTION;

        for (String nick : players) {
            boolean isActive = nick.equals(state.currentPlayer);
            boolean isOffline = offlinePlayers.contains(nick);
            boolean isViewed = nick.equals(viewedPlayerHand);
            int food = state.foodByPlayer.getOrDefault(nick, 0);
            int pp = state.ppByPlayer.getOrDefault(nick, 0);
            int picksUp = inActionResolution ? state.remainingUpper.getOrDefault(nick, 0) : 0;
            int picksLow = inActionResolution ? state.remainingLower.getOrDefault(nick, 0) : 0;

            PlayerSidebarItem pBox = new PlayerSidebarItem(
                    nick, nick.equals(state.myNickname), isActive, isOffline, isViewed,
                    food, pp, picksUp, picksLow, state.totems.get(nick), tribalFont,
                    () -> { viewedPlayerHand = nick; refreshAllInternal(lastState); }
            );
            rightSidebar.getChildren().add(pBox);
        }
    }

    /**
     * Updates the main board area with the card rows and offer tiles.
     *
     * @param state      The current state snapshot.
     * @param localDeals Output list to store tasks for new card deal animations.
     */
    private void updateMainBoard(SceneState state, List<DealTask> localDeals) {
        mainBoardArea.getChildren().clear();

        int numPlayers = Math.max(2, state.turnOrder.size());
        int boardCardHeight = 170;
        if (numPlayers == 3) boardCardHeight = 150;
        if (numPlayers == 4) boardCardHeight = 135;
        if (numPlayers >= 5) boardCardHeight = 115;

        int spacing = numPlayers >= 4 ? 6 : 12;

        // Covered building decks for future eras (rulebook 6b) float ABOVE and
        // centred over the revealed buildings (6a) of the upper row, emulating the
        // physical layout where the face-down Era II/III decks sit on top of the
        // Tribu/Edificio rows. They are stacked in a VBox over the building group;
        // the deck strip is empty (and collapses) once the final era is reached.
        HBox coveredDecks = createCoveredBuildingDecks(state, (int) (boardCardHeight * 0.7));
        VBox upperBuildingsColumn = new VBox(8); upperBuildingsColumn.setAlignment(Pos.BOTTOM_CENTER);
        if (!coveredDecks.getChildren().isEmpty()) upperBuildingsColumn.getChildren().add(coveredDecks);
        upperBuildingsColumn.getChildren().add(createCardGroup(state.upperRowBuildings, localDeals, boardCardHeight, spacing));

        // fillHeight=false keeps the character group at its natural height so it
        // bottom-aligns with the revealed buildings (same row); otherwise the HBox
        // stretches it to the taller buildings-column height and centres it, leaving
        // the characters floating above the buildings. The covered decks then sit on
        // their own row directly above the revealed buildings.
        HBox upperBand = new HBox(40); upperBand.setAlignment(Pos.BOTTOM_CENTER); upperBand.setFillHeight(false);
        upperBand.getChildren().addAll(
                createCardGroup(state.upperRow, localDeals, boardCardHeight, spacing),
                upperBuildingsColumn
        );

        HBox lowerBand = new HBox(40); lowerBand.setAlignment(Pos.CENTER);
        lowerBand.getChildren().addAll(
                createCardGroup(state.lowerRow, localDeals, boardCardHeight, spacing),
                createCardGroup(state.lowerRowBuildings, localDeals, boardCardHeight, spacing)
        );

        mainBoardArea.getChildren().addAll(upperBand, createTrackArea(state), lowerBand);
    }

    /**
     * Creates the track area containing the deck, turn order cave, and offer tiles.
     *
     * @param state The current state snapshot.
     * @return The constructed HBox representing the track area.
     */
    private HBox createTrackArea(SceneState state) {
        HBox track = new HBox(25); track.setAlignment(Pos.CENTER);
        int displayEra = state.era.ordinal() + 1;
        // The draw pile is rendered as a "mazzetto" (stacked-deck) graphic so the
        // pile reads as a stack rather than a single card. The stack image already
        // bakes in its own rounded corners and shadow, so no extra clipping here.
        String eraPath = "/images/cards/eras/deck_main_era_" + displayEra + ".png";
        currentDeckView = new ImageView(ImageLoader.getImage(eraPath));
        currentDeckView.setFitHeight(165); currentDeckView.setPreserveRatio(true);

        int numPlayers = Math.max(2, state.turnOrder.size());
        TurnOrderCaveView turnOrderCave = new TurnOrderCaveView(numPlayers, state.turnOrderSlots, model);

        HBox offerTiles = new HBox(5);
        for (OfferTileInfo t : state.offerTiles) {
            Totem occupantTotem = t.occupantNickname() != null ? state.totems.get(t.occupantNickname()) : null;
            OfferTileView tileView = new OfferTileView(t, occupantTotem, controller);
            offerTiles.getChildren().add(tileView);
        }
        track.getChildren().addAll(currentDeckView, turnOrderCave, offerTiles, new ReservePileView(tribalFont));
        return track;
    }

    /**
     * Builds the face-down building decks for the eras that have not yet entered
     * play (rulebook step 6b). At setup both the Era II and Era III building decks
     * sit covered beside the board; each is shown as a "mazzetto" stack graphic.
     * Once an era begins its buildings are revealed in the rows, so its covered
     * deck is no longer rendered — the deck for an era {@code n} appears only while
     * the current era is strictly earlier than {@code n}. The result is therefore a
     * pure function of the current era and works identically for 2–5 players.
     *
     * @param state      the current state snapshot
     * @param cardHeight the board card height, so the decks match the row scale
     * @return an HBox of covered deck views (possibly empty in the final era)
     */
    private HBox createCoveredBuildingDecks(SceneState state, int cardHeight) {
        HBox decks = new HBox(10); decks.setAlignment(Pos.CENTER);
        int displayEra = state.era.ordinal() + 1;
        for (int era = 2; era <= 3; era++) {
            if (displayEra < era) {
                ImageView deckView = new ImageView(
                        ImageLoader.getImage("/images/cards/buildings_retro/deck_build_era_" + era + ".png"));
                deckView.setFitHeight(cardHeight); deckView.setPreserveRatio(true);
                decks.getChildren().add(deckView);
            }
        }
        return decks;
    }

    /**
     * Creates a group of card views for a row.
     *
     * @param ids        The card IDs to include in the group.
     * @param localDeals Output list to store tasks for new card deal animations.
     * @return The constructed HBox representing the card group.
     */
    private HBox createCardGroup(List<String> ids, List<DealTask> localDeals) {
        HBox hb = new HBox(12); hb.setAlignment(Pos.CENTER);
        if (ids != null) {
            for (String id : ids) {
                boolean isNew = !knownCardsOnBoard.contains(id);
                if (isNew) knownCardsOnBoard.add(id);

                GameCardView cardView = new GameCardView(id, selected.contains(id), 170, () -> toggleCardSelection(id));
                if (isNew) {
                    cardView.getCardImageView().setOpacity(0.0);
                    localDeals.add(new DealTask(id, cardView.getCardImageView()));
                }
                hb.getChildren().add(cardView);
            }
        }
        return hb;
    }

    /**
     * Updates the hand display for the currently viewed player.
     *
     * @param state The current state snapshot.
     */
    private void updateHandDisplay(SceneState state) {
        handCardsBox.getChildren().clear();
        handTitle.setText(viewedPlayerHand + "'s TRIBE");

        List<String> chars = state.charactersByPlayer.getOrDefault(viewedPlayerHand, List.of());
        List<String> buildings = state.buildingsByPlayer.getOrDefault(viewedPlayerHand, List.of());
        List<String> allCards = new ArrayList<>(chars); allCards.addAll(buildings);

        String[] columnOrder = {"INVENTOR", "BUILDER", "GATHERER", "ARTIST", "SHAMAN", "HUNTER", "BUILDING"};
        for (String category : columnOrder) {
            List<String> cardsInCategory = allCards.stream().filter(id -> extractCategoryFromId(id).equals(category)).collect(Collectors.toList());
            handCardsBox.getChildren().add(createCascadingStack(category, cardsInCategory));
        }
    }

    /**
     * Creates a cascading stack of card views for a specific category in the hand.
     *
     * @param category The card category.
     * @param cardIds  The list of card IDs in this category.
     * @return The constructed StackPane representing the cascading stack.
     */
    private StackPane createCascadingStack(String category, List<String> cardIds) {
        StackPane stack = new StackPane(); stack.setAlignment(Pos.TOP_CENTER);
        if (cardIds.isEmpty()) { stack.setPrefWidth(90); return stack; }
        // Cascade cards downward, but cap the total spread to a fixed budget so large stacks
        // (e.g. dozens of cards of the same type) stay within the hand area instead of overflowing
        // past the clipped, non-scrolling viewport. With few cards the offset stays at the full 25px.
        // The budget plus the card height below must fit the fixed strip height (see the
        // handCardsBox minHeight / mainBoardArea bottom reservation) so the strip never grows.
        int spreadBudget = 30;
        int offsetPerCard = cardIds.size() <= 1 ? 0 : Math.min(25, spreadBudget / (cardIds.size() - 1));
        stack.setPadding(new Insets(20, 0, Math.max(0, cardIds.size() - 1) * offsetPerCard, 0));

        for (int i = 0; i < cardIds.size(); i++) {
            String id = cardIds.get(i);
            GameCardView cardView = new GameCardView(id, false, 105, () -> showZoomedCard(id));
            cardView.setTranslateY(i * offsetPerCard); stack.getChildren().add(cardView);
        }

        int totaleStelle = 0; int totaleScontoEdifici = 0; int totalePP = 0; int totaleScontoCibo = 0; long simboliInventoreUnici = 0;
        if (category.equals("SHAMAN")) totaleStelle = cardIds.stream().mapToInt(this::extractShamanStars).sum();
        else if (category.equals("BUILDER")) { totaleScontoEdifici = cardIds.stream().mapToInt(this::extractBuilderDiscount).sum(); totalePP = cardIds.stream().mapToInt(this::extractPrestigePoints).sum(); }
        else if (category.equals("GATHERER")) totaleScontoCibo = cardIds.stream().mapToInt(this::extractGathererDiscount).sum();
        else if (category.equals("INVENTOR")) simboliInventoreUnici = cardIds.stream().map(this::extractInventorSymbol).filter(s -> !s.isEmpty()).distinct().count();

        String countBadge = "(x" + cardIds.size() + ")"; String infoText = "";
        if (category.equals("SHAMAN") && totaleStelle > 0) infoText = countBadge + " | " + totaleStelle + " ⭐";
        else if (category.equals("BUILDER")) infoText = countBadge + " | -" + totaleScontoEdifici + " Cost | " + totalePP + " PP";
        else if (category.equals("GATHERER") && totaleScontoCibo > 0) infoText = countBadge + " | -" + totaleScontoCibo + " Food";
        else if (category.equals("INVENTOR") && simboliInventoreUnici > 0) infoText = countBadge + " | " + simboliInventoreUnici + " 💡";
        else infoText = category.substring(0, Math.min(3, category.length())) + " " + countBadge;

        Label infoBadge = new Label(infoText);
        infoBadge.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-text-fill: gold; -fx-padding: 3 8; -fx-background-radius: 5; -fx-border-color: gold; -fx-border-radius: 5;");
        if (tribalFont != null) infoBadge.setFont(Font.font(tribalFont.getFamily(), 10));
        infoBadge.setTranslateY(-15); StackPane.setAlignment(infoBadge, Pos.TOP_CENTER);
        stack.getChildren().add(infoBadge);
        return stack;
    }

    /**
     * Creates a card deal animation from the deck to a target position.
     *
     * @param id     The card ID.
     * @param target The target ImageView where the card will end up.
     * @param era    The current era index.
     * @return The constructed Animation instance.
     */
    private Animation createSingleDealAnimation(String id, ImageView target, int era) {
        if (currentDeckView == null || currentDeckView.getScene() == null || target.getScene() == null) {
            target.setOpacity(1.0); return new PauseTransition(Duration.millis(1));
        }
        javafx.geometry.Point2D start = currentDeckView.localToScene(0, 0);
        javafx.geometry.Point2D end = target.localToScene(0, 0);
        int displayEra = Math.min(era, 3);

        double targetHeight = target.getFitHeight() > 0 ? target.getFitHeight() : 170;

        ImageView fly = new ImageView(ImageLoader.getImage("/images/cards/eras/back_main_era_" + displayEra + ".png"));
        fly.setFitHeight(targetHeight);
        fly.setPreserveRatio(true); fly.setManaged(false);
        applyRoundedCorners(fly, 10);
        fly.relocate(start.getX(), start.getY()); baseStack.getChildren().add(fly);

        TranslateTransition tt = new TranslateTransition(Duration.millis(700), fly);
        tt.setToX(end.getX() - start.getX()); tt.setToY(end.getY() - start.getY());

        ScaleTransition st1 = new ScaleTransition(Duration.millis(350), fly); st1.setToX(0);
        st1.setOnFinished(e -> {
            String sub = id.startsWith("C_") ? "characters/" : id.startsWith("B_") ? "buildings/" : "events/";
            fly.setImage(ImageLoader.getImage("/images/cards/" + sub + id + ".png"));
            ScaleTransition st2 = new ScaleTransition(Duration.millis(350), fly);
            st2.setToX(1.0); st2.play();
        });

        ParallelTransition pt = new ParallelTransition(tt, st1);
        pt.setOnFinished(e -> baseStack.getChildren().remove(fly));
        return pt;
    }

    /**
     * Animates a card being taken from the board by a player.
     *
     * @param nickname The nickname of the player taking the card.
     * @param cardID   The card ID.
     * @param type     The type of card.
     * @param source   The row position from which the card was taken.
     */
    public void animateCardTaken(String nickname, String cardID, CardType type, RowPosition source) {
        Platform.runLater(() -> {
            animationQueue.add(() -> executeCardTakenAnimation(nickname, cardID, source));
            if (!isAnimating) playNextAnimation();
        });
    }

    /**
     * Executes the card taken animation.
     *
     * @param nickname The nickname of the player.
     * @param cardID   The card ID.
     * @param source   The row position.
     */
    private void executeCardTakenAnimation(String nickname, String cardID, RowPosition source) {
        String sub = cardID.startsWith("C_") ? "characters/" : cardID.startsWith("B_") ? "buildings/" : "events/";
        ImageView fly = new ImageView(ImageLoader.getImage("/images/cards/" + sub + cardID + ".png"));
        fly.setFitHeight(170); fly.setPreserveRatio(true); fly.setTranslateY(source == RowPosition.UPPER ? -150 : 150);
        applyRoundedCorners(fly, 10);
        baseStack.getChildren().add(fly);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(1), fly);
        if (nickname.equals(model.getMyNickname())) { tt.setToY(350); tt.setToX(-200); }
        else { tt.setToY(-250); tt.setToX(500); }

        ScaleTransition st = new ScaleTransition(Duration.seconds(1), fly); st.setToX(0.1); st.setToY(0.1);
        FadeTransition ft = new FadeTransition(Duration.seconds(1), fly); ft.setToValue(0);

        ParallelTransition pt = new ParallelTransition(tt, st, ft);
        pt.setOnFinished(e -> {
            baseStack.getChildren().remove(fly); knownCardsOnBoard.remove(cardID);
            playNextAnimation();
        });
        pt.play();
    }

    /**
     * Animates the placement of a totem on an offer tile.
     *
     * @param nickname The nickname of the player.
     * @param tileID   The ID of the offer tile.
     */
    public void animateTotemPlacement(String nickname, char tileID) {
        Totem totem = model.getTotem(nickname);
        Platform.runLater(() -> {
            animationQueue.add(() -> executeTotemAnimation(nickname, tileID, false, totem));
            if (!isAnimating) playNextAnimation();
        });
    }

    /**
     * Animates the return of a totem to the turn order cave.
     *
     * @param nickname The nickname of the player.
     * @param position The position index.
     */
    public void animateTotemReturn(String nickname, int position) {
        Totem totem = model.getTotem(nickname);
        Platform.runLater(() -> {
            animationQueue.add(() -> executeTotemAnimation(nickname, 'T', true, totem));
            if (!isAnimating) playNextAnimation();
        });
    }

    /**
     * Executes the totem move animation.
     *
     * @param nickname    The nickname of the player.
     * @param tileID      The target tile ID.
     * @param isReturning True if the totem is returning to the cave.
     * @param totem       The totem color.
     */
    private void executeTotemAnimation(String nickname, char tileID, boolean isReturning, Totem totem) {
        if (totem == null) { playNextAnimation(); return; }

        String path = "/images/totems/totem_" + totem.name().toLowerCase() + ".png";
        ImageView fly = new ImageView(ImageLoader.getImage(path));
        fly.setFitHeight(120); fly.setPreserveRatio(true); fly.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 5);");

        Label actionLabel = new Label(); actionLabel.setTextFill(Color.GOLD);
        if (tribalFont != null) actionLabel.setFont(Font.font(tribalFont.getFamily(), 20));
        actionLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: gold; -fx-border-radius: 10;");

        if (isReturning) {
            actionLabel.setText(nickname + " RETURNS TO CAVE"); fly.setTranslateY(-300);
        } else {
            actionLabel.setText(nickname + " PLACES ON TILE " + tileID); fly.setTranslateY(300);
        }

        VBox animBox = new VBox(20, fly, actionLabel); animBox.setAlignment(Pos.CENTER);
        baseStack.getChildren().add(animBox);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), fly); tt.setToY(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), fly); st.setByX(0.3); st.setByY(0.3); st.setAutoReverse(true); st.setCycleCount(2);
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        FadeTransition ft = new FadeTransition(Duration.millis(300), animBox); ft.setToValue(0);

        SequentialTransition seq = new SequentialTransition(tt, st, pause, ft);
        seq.setOnFinished(e -> { baseStack.getChildren().remove(animBox); playNextAnimation(); });
        seq.play();
    }

    /**
     * Toggles the selection of a card on the board.
     *
     * @param id The card ID.
     */
    private void toggleCardSelection(String id) {
        if (isAnimating || !cardActionsAllowed()) return;
        if (selected.contains(id)) selected.remove(id); else selected.add(id);
        refreshAllInternal(lastState);
    }

    /**
     * Displays a zoomed-in view of a card.
     *
     * @param cardId The card ID.
     */
    private void showZoomedCard(String cardId) {
        StackPane overlay = new StackPane(); overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        String sub = cardId.startsWith("C_") ? "characters/" : cardId.startsWith("B_") ? "buildings/" : "events/";
        String path = "/images/cards/" + sub + cardId + ".png";
        ImageView bigCard = new ImageView(ImageLoader.getImage(path)); bigCard.setFitHeight(550); bigCard.setPreserveRatio(true);
        applyRoundedCorners(bigCard, 25);
        bigCard.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 40, 0.4, 0, 0);");
        bigCard.setScaleX(0.5); bigCard.setScaleY(0.5);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), bigCard); st.setToX(1.0); st.setToY(1.0); st.play();
        overlay.setOnMouseClicked(e -> baseStack.getChildren().remove(overlay));
        overlay.getChildren().add(bigCard); baseStack.getChildren().add(overlay);
    }

    /**
     * Extracts the category of a card from its ID.
     *
     * @param cardId The card ID.
     * @return The category string.
     */
    private String extractCategoryFromId(String cardId) {
        if (cardId.startsWith("B_")) return "BUILDING";
        String cardInfo = CardCatalog.getInstance().format(cardId).toUpperCase();
        if (cardInfo.contains("INVENTOR")) return "INVENTOR"; if (cardInfo.contains("BUILDER")) return "BUILDER";
        if (cardInfo.contains("GATHERER")) return "GATHERER"; if (cardInfo.contains("ARTIST")) return "ARTIST";
        if (cardInfo.contains("SHAMAN")) return "SHAMAN"; if (cardInfo.contains("HUNTER")) return "HUNTER";
        return "UNKNOWN";
    }

    /**
     * Extracts prestige points from a card description.
     */
    private int extractPrestigePoints(String cardId) { String desc = CardCatalog.getInstance().format(cardId); int index = desc.indexOf("PP:"); if (index != -1) { int end = desc.indexOf(" ", index + 3); if (end == -1) end = desc.length(); try { return Integer.parseInt(desc.substring(index + 3, end).trim()); } catch (Exception ignored) {} } return 0; }
    
    /**
     * Extracts inventor symbol from a card description.
     */
    private String extractInventorSymbol(String cardId) { String desc = CardCatalog.getInstance().format(cardId); int start = desc.indexOf(" ("); if (start != -1) { int end = desc.indexOf(")", start); if (end != -1) return desc.substring(start + 2, end).trim(); } return ""; }
    
    /**
     * Extracts shaman stars from a full card description.
     */
    private int extractShamanStars(String cardId) { String desc = CardCatalog.getInstance().getFullDescription(cardId); int index = desc.indexOf("Shaman Stars: "); if (index != -1) { int end = desc.indexOf("\n", index + 14); if (end != -1) { try { return Integer.parseInt(desc.substring(index + 14, end).trim()); } catch (Exception ignored) {} } } return 0; }
    
    /**
     * Extracts builder discount from a full card description.
     */
    private int extractBuilderDiscount(String cardId) { String desc = CardCatalog.getInstance().getFullDescription(cardId); int index = desc.indexOf("Building Discount: -"); if (index != -1) { int end = desc.indexOf(" Food", index + 20); if (end != -1) { try { return Integer.parseInt(desc.substring(index + 20, end).trim()); } catch (Exception ignored) {} } } return 0; }
    
    /**
     * Extracts gatherer discount from a full card description.
     */
    private int extractGathererDiscount(String cardId) { String desc = CardCatalog.getInstance().getFullDescription(cardId); int index = desc.indexOf("Food Discount: -"); if (index != -1) { int end = desc.indexOf(" Food", index + 16); if (end != -1) { try { return Integer.parseInt(desc.substring(index + 16, end).trim()); } catch (Exception ignored) {} } } return 0; }

    /**
     * Displays an animation for the beginning of a new era.
     *
     * @param newEra               The new era.
     * @param u                    New upper row buildings.
     * @param l                    New lower row buildings.
     */
    public void showNewEraAnimation(Era newEra, List<String> u, List<String> l) {
        refreshAll(model);

        Platform.runLater(() -> {
            animationQueue.add(() -> {
                root.setEffect(new GaussianBlur(12));

                NewEraOverlay overlay = new NewEraOverlay();
                StackPane node = overlay.buildNode(newEra, () -> {
                    root.setEffect(null);
                    modalLayer.setVisible(false);
                    modalLayer.getChildren().clear();
                    playNextAnimation();
                });
                modalLayer.getChildren().setAll(node);
                modalLayer.setVisible(true);
            });
            if (!isAnimating) playNextAnimation();
        });
    }

    /**
     * Marks a player as offline and refreshes the scene.
     *
     * @param n The player nickname.
     */
    public void setPlayerOffline(String n) { if(!offlinePlayers.contains(n)) offlinePlayers.add(n); refreshAll(model); }
    
    /**
     * Marks a player as online and refreshes the scene.
     *
     * @param n The player nickname.
     */
    public void setPlayerOnline(String n) { offlinePlayers.remove(n); refreshAll(model); }

    public void showForfeitBanner(long seconds) {
        Platform.runLater(() -> {
            if (forfeitBanner != null) baseStack.getChildren().remove(forfeitBanner);

            Label icon = new Label("⚠");
            icon.setStyle("-fx-font-size: 32; -fx-text-fill: #FFD700;");

            Label titleLabel = new Label("YOU ARE THE ONLY ACTIVE PLAYER");
            titleLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 15;");
            if (tribalFont != null) titleLabel.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 15));

            // Deliberately "about Ns", not a live countdown: the server fires the
            // notification at the END of the catch-up replay and the FX queue may be
            // backed up, so a precise client-side counter would drift behind the
            // server's authoritative deadline. The approximate value uses the
            // server-supplied {@code seconds} so it tracks any future timeout change.
            Label countdownLabel = new Label("Win by forfeit in about " + seconds + "s if no one reconnects.");
            countdownLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
            if (tribalFont != null) countdownLabel.setFont(Font.font(tribalFont.getFamily(), 13));

            forfeitBanner = new VBox(8, icon, titleLabel, countdownLabel);
            forfeitBanner.setAlignment(Pos.CENTER);
            forfeitBanner.setStyle(
                    "-fx-background-color: rgba(163, 29, 29, 0.92);" +
                            "-fx-border-color: #FFD700;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 20 35 20 35;"
            );
            forfeitBanner.setMaxSize(420, Region.USE_PREF_SIZE);
            StackPane.setAlignment(forfeitBanner, Pos.CENTER);

            baseStack.getChildren().add(forfeitBanner);
            forfeitBanner.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), forfeitBanner);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            addLogEntry("[!] ", "You are alone — forfeit win in about " + seconds + "s if no one reconnects.", Color.web("#FF4444"));
        });
    }

    public void dismissForfeitBanner() {
        Platform.runLater(() -> {
            if (forfeitBanner != null) {
                VBox bannerRef = forfeitBanner;
                forfeitBanner = null;
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), bannerRef);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> baseStack.getChildren().remove(bannerRef));
                fadeOut.play();
                addLogEntry("[!] ", "A player reconnected — forfeit countdown cancelled.", Color.web("#4CAF50"));
            }
        });
    }

    public void logPlayerReconnected(String nickname) {
        Platform.runLater(() -> {
            addLogEntry("[✓] ", nickname + " has reconnected!", Color.web("#4CAF50"));

            Label icon = new Label("✓");
            icon.setStyle("-fx-font-size: 28; -fx-text-fill: #4CAF50;");

            Label msg = new Label(nickname + " HAS RECONNECTED!");
            msg.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
            if (tribalFont != null) msg.setFont(Font.font(tribalFont.getFamily(), FontWeight.BOLD, 14));

            VBox banner = new VBox(6, icon, msg);
            banner.setAlignment(Pos.CENTER);
            banner.setStyle(
                    "-fx-background-color: rgba(30, 100, 30, 0.95);" +
                            "-fx-border-color: #4CAF50;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 18 35 18 35;"
            );
            banner.setMaxSize(380, Region.USE_PREF_SIZE);
            StackPane.setAlignment(banner, Pos.CENTER);
            banner.setOpacity(0);
            baseStack.getChildren().add(banner);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), banner);
            fadeIn.setToValue(1.0);
            PauseTransition hold = new PauseTransition(Duration.seconds(3));
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), banner);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> baseStack.getChildren().remove(banner));

            new SequentialTransition(fadeIn, hold, fadeOut).play();
        });
    }

    /**
     * Creates a group of card views with specific sizing and spacing.
     *
     * @param ids        The card IDs.
     * @param localDeals Output list for deal tasks.
     * @param cardHeight The height for cards.
     * @param spacing    The spacing between cards.
     * @return The constructed HBox.
     */
    private HBox createCardGroup(List<String> ids, List<DealTask> localDeals, int cardHeight, int spacing) {
        HBox hb = new HBox(spacing); hb.setAlignment(Pos.CENTER);
        if (ids != null) {
            for (String id : ids) {
                boolean isNew = !knownCardsOnBoard.contains(id);
                if (isNew) knownCardsOnBoard.add(id);

                GameCardView cardView = new GameCardView(id, selected.contains(id), cardHeight, () -> toggleCardSelection(id));

                if (isNew) {
                    cardView.getCardImageView().setOpacity(0.0);
                    localDeals.add(new DealTask(id, cardView.getCardImageView()));
                }
                hb.getChildren().add(cardView);
            }
        }
        return hb;
    }

    /**
     * Prepares the scene for replaying events (e.g., during reconnection).
     */
    public void prepareForReplay() {
        this.knownCardsOnBoard.clear();
        this.selected.clear();
        this.animationQueue.clear();
        this.isAnimating = false;
        this.isReplaying = true;
        Platform.runLater(this::enterReplayUiState);
    }

    /**
     * Locks the interactive controls (board cards and action buttons) for the
     * duration of the catch-up replay and arms the watchdog that detects when
     * the fast-forward event stream has gone quiet.
     */
    private void enterReplayUiState() {
        // Wipe the log drawer so the fast-forward repopulates it from scratch,
        // mirroring the TUI which re-prints the history on catch-up. Without this
        // the pre-disconnect entries linger at the top while the replayed entries
        // get pushed out by the 10-line cap, leaving the drawer "stuck on the old
        // logs". Running here (on the FX thread, queued at the first replay event)
        // guarantees the clear happens before any replayed log line is appended.
        // No marker line is added: this path also runs on a normal game start,
        // where a "recovered/replaying" note would be misleading.
        if (logContainer != null) logContainer.getChildren().clear();
        updateActionButtonsState();
        armReplayWatchdog();
    }

    /**
     * (Re)starts the quiet-period timer. Each replayed event resets it; once no
     * further event arrives within the window the replay is considered complete
     * and the controls are unlocked. The window is generous on purpose: a long
     * catch-up can have multi-second gaps between bursts of events, and unlocking
     * too early would let the player act against a stale frame of the replay.
     */
    private void armReplayWatchdog() {
        if (replayWatchdog == null) {
            replayWatchdog = new PauseTransition(Duration.seconds(3));
            replayWatchdog.setOnFinished(e -> finishReplay());
        }
        replayWatchdog.playFromStart();
    }

    /** Unlocks the controls once the catch-up replay has finished. */
    private void finishReplay() {
        isReplaying = false;
        updateActionButtonsState();
    }

    /**
     * @return {@code true} if the local player may currently issue an action,
     *         i.e. the catch-up replay is over and it is their turn.
     */
    private boolean actionsAllowed() {
        return !isReplaying && isMyTurn(lastState);
    }

    /**
     * @return {@code true} if the local player may currently select board cards
     *         and confirm a pick. Card actions belong exclusively to the
     *         {@link PhaseType#ACTION_RESOLUTION} phase; during
     *         {@link PhaseType#TOTEM_PLACEMENT} (and any other phase) the player
     *         places their totem on an offer tile instead, so cards must not be
     *         selectable and CONFIRM PICK must stay disabled.
     */
    private boolean cardActionsAllowed() {
        return actionsAllowed() && lastState != null
                && lastState.currentPhase == PhaseType.ACTION_RESOLUTION;
    }

    /** @return {@code true} if it is the local player's turn in the given state. */
    private boolean isMyTurn(SceneState s) {
        return s != null && s.myNickname != null && s.myNickname.equals(s.currentPlayer);
    }

    /**
     * Enables CONFIRM PICK / END TURN only when an action is actually allowed
     * (not replaying and it is the local player's turn); disables them otherwise.
     * The button state is thus a pure function of the current scene state and is
     * refreshed on every render — during normal play this simply tracks whose
     * turn it is.
     */
    private void updateActionButtonsState() {
        boolean allowed = actionsAllowed();
        // CONFIRM PICK belongs to ACTION_RESOLUTION only: during TOTEM_PLACEMENT
        // (or any other phase) it must stay disabled even on the player's turn.
        if (confirmBtn != null) confirmBtn.setDisable(!cardActionsAllowed());
        if (endTurnBtn != null) endTurnBtn.setDisable(!allowed);
    }

    /**
     * Applies rounded corners to an ImageView.
     *
     * @param imageView The ImageView to clip.
     * @param radius    The corner radius.
     */
    private void applyRoundedCorners(ImageView imageView, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius);
        clip.setArcHeight(radius);
        clip.setWidth(imageView.getBoundsInLocal().getWidth());
        clip.setHeight(imageView.getBoundsInLocal().getHeight());
        imageView.boundsInLocalProperty().addListener((obs, oldVal, newVal) -> {
            clip.setWidth(newVal.getWidth());
            clip.setHeight(newVal.getHeight());
        });
        imageView.setClip(clip);
    }
}