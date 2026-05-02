package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.view.CardCatalog;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.AbstractClientView;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.*;

/**
 * Text-based user interface for the MESOS game client.
 *
 * <p>Renders the complete game state to the terminal using ANSI escape codes.
 * Handles both lobby and in-game phases.</p>
 *
 * <h2>Reconnection strategy</h2>
 * <p>When a player reconnects, the {@code GameController} replays the full
 * event history ("fast-forward"). Each event updates {@link GameModel} state
 * incrementally. The view does <em>not</em> attempt to render during fast-forward
 * because {@link #setGameModel(GameModel)} has not been called yet (or
 * {@code gameModel} is still being populated). As soon as the controller calls
 * {@code setGameModel(populatedModel)}, this view immediately performs a full
 * repaint — showing cards, resources, and turn order exactly as they are.</p>
 *
 * <h2>Notification buffer</h2>
 * <p>Only the last {@value #MAX_NOTIFICATIONS} log lines are kept; older ones
 * are silently discarded.</p>
 */
public class TuiView extends AbstractClientView {

    // -----------------------------------------------------------------------
    // ANSI constants
    // -----------------------------------------------------------------------

    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE   = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN   = "\u001B[36m";
    private static final String WHITE  = "\u001B[37m";
    private static final String BOLD   = "\u001B[1m";

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    /** Maximum number of recent log entries shown on screen. */
    private static final int MAX_NOTIFICATIONS = 10;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final LobbyModel lobbyModel;
    private GameModel gameModel;

    /** Fallback game ID used before {@link GameModel} is fully populated. */
    private String currentGameId;

    /** Ring buffer of recent log lines. */
    private final LinkedList<String> notifications = new LinkedList<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a new {@code TuiView} bound to the given lobby model.
     *
     * @param lobbyModel the client-side lobby model; must not be {@code null}
     */
    public TuiView(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
        lobbyModel.addObserver(this);
    }

    // -----------------------------------------------------------------------
    // GameModel injection — key reconnection hook
    // -----------------------------------------------------------------------

    /**
     * Binds the populated game model and immediately forces a full repaint.
     *
     * <p>This is the single hook that makes reconnection work: the controller
     * calls this method <em>after</em> the fast-forward event sequence has
     * fully updated the model. The first render therefore always reflects the
     * complete, current game state — cards taken, resources, turn order, etc.</p>
     *
     * @param gameModel the populated client-side game model; {@code null} resets
     *                  the view to the pre-game state
     */
    @Override
    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
        if (this.gameModel != null) {
            renderFullGame();
        }
    }

    // -----------------------------------------------------------------------
    // Notification helper
    // -----------------------------------------------------------------------

    /**
     * Appends a message to the notification ring buffer and triggers a repaint.
     * If {@link #gameModel} is not yet available, prints the message inline.
     *
     * @param message ANSI-formatted log line
     */
    private void addNotification(String message) {
        notifications.addLast(message);
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.removeFirst();
        }
        if (gameModel != null) {
            renderFullGame();
        } else {
            System.out.println(message);
            System.out.print("\n" + CYAN + "> " + RESET);
        }
    }

    // -----------------------------------------------------------------------
    // LOBBY CALLBACKS
    // -----------------------------------------------------------------------

    @Override
    public void onUsernameResult(String username, boolean accepted, String reason) {
        if (accepted) {
            clearScreen();
            printHeader();
            System.out.println(GREEN + "Successfully logged in as: " + BOLD + username + RESET);
            System.out.println("\nCommands: create <size> | join <index> | quit");
        } else {
            System.out.println(RED + "[ERROR] Username '" + username + "' rejected: " + reason + RESET);
        }
        System.out.print("\n" + CYAN + "> " + RESET);
    }

    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        if (gameModel != null) return;
        clearScreen();
        printHeader();
        System.out.println(YELLOW + "Logged in as: " + BOLD + lobbyModel.getMyNickname() + RESET);
        System.out.println("\n" + PURPLE + BOLD + "--- AVAILABLE LOBBIES ---" + RESET);
        if (lobbies.isEmpty()) {
            System.out.println("  No active lobbies found. Use 'create <size>' to start one.");
        } else {
            for (int i = 0; i < lobbies.size(); i++) {
                LobbyInfo info = lobbies.get(i);
                System.out.printf("  [%d] Players: %d/%d%n",
                        i, info.currentPlayers().size(), info.expectedPlayers());
            }
        }
        System.out.println("\nCommands: create <2-5> | join <index> | reconnect <nick> <id> | quit");
        System.out.print("\n" + CYAN + "> " + RESET);
    }

    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        if (gameModel != null) return;
        clearScreen();
        printHeader();
        System.out.println(PURPLE + BOLD + "--- LOBBY: " + lobby.lobbyId() + " ---" + RESET);
        System.out.printf("Capacity: %d/%d players%n",
                lobby.currentPlayers().size(), lobby.expectedPlayers());
        System.out.println("\nPlayers:");

        lobby.currentPlayers().forEach(n -> {
            Totem chosen = lobby.chosenTotems().get(n);
            String totemStr = (chosen != null)
                    ? YELLOW + "[" + chosen + "]"
                    : RED + "[no totem selected]";
            System.out.println("  • " + n + " " + totemStr + RESET);
        });

        // CALCOLO T
        List<Totem> availableTotems = new ArrayList<>(Arrays.asList(Totem.values()));
        availableTotems.removeAll(lobby.chosenTotems().values());

        System.out.println("\n" + CYAN + "Available totems: " + RESET + availableTotems);

        // --- CONDITIONAL COMMAND LOGIC ---
        String myNick = lobbyModel.getMyNickname(); // Retrieve local nickname

        if (myNick == null || myNick.isBlank()) {
            // Player hasn't set a nickname yet
            System.out.println("\n" + YELLOW + BOLD + ">> STEP 1: Enter a nickname to join" + RESET);
            System.out.println("Commands: nick <name> | leave | quit");
        } else {
            // Nickname set, check if they already have a totem
            boolean hasTotem = lobby.chosenTotems().containsKey(myNick);
            String totemCmd = hasTotem ? "totem <color> (to change)" : "totem <color>";
            String statusMsg = hasTotem ? "You are ready!" : "Pick your totem!";

            System.out.println("\n" + GREEN + BOLD + ">> STEP 2: Nickname set (" + myNick + "). " + statusMsg + RESET);
            System.out.println("Commands: nick <name> (to change) | " + totemCmd + " | totems | leave | quit");
        }
        // -----------------------------------------

        System.out.print("\n" + CYAN + "> " + RESET);
    }

    @Override
    public void onShowAvailableTotems() {
        LobbyInfo lobby = lobbyModel.getCurrentLobby();
        if (lobby != null) {
            List<Totem> available = new ArrayList<>(Arrays.asList(Totem.values()));
            available.removeAll(lobby.chosenTotems().values());

            addNotification(CYAN + "[LOBBY] Available totems: " + GREEN + available + RESET);
        }
    }

    @Override
    public void onLobbyDissolved() {
        System.out.println("\n" + RED + "[LOBBY] The lobby has been dissolved." + RESET);
        System.out.print("\n" + CYAN + "> " + RESET);
    }

    // -----------------------------------------------------------------------
    // GAME CALLBACKS
    // -----------------------------------------------------------------------

    @Override
    public void onGameStarted(String gameId) {
        this.currentGameId = gameId;
        lobbyModel.removeObserver(this);
        addNotification(GREEN + BOLD + "GAME STARTED! ID: " + gameId + RESET);
    }

    @Override
    public void onGameSetupCompleted(List<String> turnOrder,
                                     Map<String, Integer> initialFood,
                                     BoardSnapshot board) {
        renderFullGame();
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> order) {
        renderFullGame();
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        addNotification(BLUE + "[TURN] It's now " + BOLD + nextPlayer + RESET
                + BLUE + "'s turn." + RESET);
    }

    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) {
        renderFullGame();
    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        addNotification(WHITE + "[BOARD] " + nickname
                + " placed their totem on tile " + BOLD + tileID + RESET);
    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        // turnOrderPosition is 0-based from the server; display as 1-based
        addNotification(WHITE + "[BOARD] " + nickname
                + " returned to turn-order slot " + (turnOrderPosition + 1) + RESET);
    }

    @Override
    public void onOfferTilesUpdated(List<OfferTileInfo> tiles) {
        renderFullGame();
    }

    @Override
    public void onBoardUpdated(List<String> newUpper, List<String> newLower, int deckCount) {
        renderFullGame();
    }

    @Override
    public void onEraChanged(List<String> newUpperBuildings, List<String> newLowerBuildings) {
        addNotification(PURPLE + BOLD + "[ERA] A new era has begun!" + RESET);
        renderFullGame();
    }

    @Override
    public void onPlayerLimitsInitialized(String nickname, int upper, int lower) {
        renderFullGame();
    }

    @Override
    public void onPlayerLimitsUpdated(String nickname, int upper, int lower) {
        renderFullGame();
    }

    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
        addNotification(YELLOW + "[RESOURCE] " + nickname
                + " now has " + newValue + " " + resource + RESET);
    }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        String formatted = CardCatalog.getInstance().format(cardID);
        addNotification(CYAN + "[ACTION] " + nickname + " took " + formatted + RESET);
    }

    @Override
    public void onEventResolved(String eventID, String eventName) {
        addNotification(PURPLE + "[EVENT] Resolved: " + eventName + RESET);
    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        addNotification(GREEN + "[EXTRA] " + nickname + " gained an extra turn!"
                + " (Up:" + remainingUpper + " Lw:" + remainingLower + ")" + RESET);
    }

    @Override
    public void onExtraTurnEnded(String nickname) {
        addNotification(YELLOW + "[EXTRA] " + nickname + "'s extra turn ended." + RESET);
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        addNotification(RED + BOLD + "[!] Player disconnected: " + nickname + RESET);
    }

    @Override
    public void onPlayerReconnected(String nickname) {
        addNotification(GREEN + "[!] Player reconnected: " + nickname + RESET);
    }

    @Override
    public void onError(String message) {
        addNotification(RED + "[ERROR] " + message + RESET);
    }

    @Override
    public void onConnectionLost() {
        clearScreen();
        printHeader();
        System.out.println(RED + BOLD + "=== SERVER CONNECTION LOST ===" + RESET);
        System.out.println(YELLOW + "The server is currently offline or unreachable." + RESET);
        System.out.println("Please wait. The client will attempt to reconnect automatically...\n");

        // Recupera l'ID del gioco se disponibile
        String idToPrint = (gameModel != null && gameModel.getGameId() != null)
                ? gameModel.getGameId()
                : currentGameId;

        if (idToPrint != null) {
            System.out.println(CYAN + "Your Game ID (in case you need to reconnect later): " + BOLD + idToPrint + RESET);
        }
    }

    @Override
    public void onConnectionRestored() {
        clearScreen();
        printHeader();
        System.out.println(GREEN + BOLD + "=== CONNECTION RESTORED ===" + RESET);
        System.out.println("Successfully reconnected to the server!");
        System.out.println("Resynchronizing state, please wait...\n");
    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        clearScreen();
        printHeader();
        System.out.println(GREEN + BOLD + "=== GAME OVER ===" + RESET);
        System.out.println("Winners: " + String.join(", ", winners));
        System.out.println("\n" + PURPLE + "--- FINAL RANKINGS ---" + RESET);
        for (int i = 0; i < finalRankings.size(); i++) {
            PlayerFinalScore s = finalRankings.get(i);
            System.out.printf("  %d. %-15s  PP: %d%n",
                    i + 1, s.nickname(), s.totalPrestigePoints());
        }
        System.out.println("\nType 'lobby' to return to lobby, or 'quit' to exit.");
        System.out.print("\n" + CYAN + "> " + RESET);
    }

    @Override
    public void onGameAborted(String lastManStanding) {
        clearScreen();
        printHeader();
        System.out.println(RED + BOLD + "=== GAME ABORTED ===" + RESET);
        System.out.println("Winner by forfeit: " + lastManStanding);
        System.out.println("\nType 'lobby' to return to lobby, or 'quit' to exit.");
        System.out.print("\n" + CYAN + "> " + RESET);
    }

    @Override
    public void onGameRecoveryFailed() {
        clearScreen();
        printHeader();
        System.out.println(RED + BOLD + "=== GAME RECOVERY FAILED ===" + RESET);
        System.out.println("Not all players reconnected in time. The game has been terminated.");
        System.out.println("\nType 'lobby' to return to lobby, or 'quit' to exit.");
        System.out.print("\n" + CYAN + "> " + RESET);
    }

    @Override
    public void onReturnToLobby() {
        this.currentGameId = null;
        lobbyModel.addObserver(this);
        this.gameModel = null;
        clearScreen();
        printHeader();
        System.out.println(GREEN + "You are back in the lobby." + RESET);
        System.out.println("\nCommands: create <size> | join <index> | quit");
        System.out.print("\n" + CYAN + "> " + RESET);
    }

    // -----------------------------------------------------------------------
    // CORE RENDERING ENGINE
    // -----------------------------------------------------------------------

    /**
     * Clears the screen and repaints the complete in-game UI from the current
     * {@link GameModel} state. No-op if the model is not yet available.
     */
    private void renderFullGame() {
        if (gameModel == null || gameModel.getGameId() == null) {
            return;
        }
        clearScreen();
        printHeader();

        PhaseType phase  = gameModel.getCurrentPhase();
        String currentP  = gameModel.getCurrentPlayer();
        String displayId = gameModel.getGameId() != null
                ? gameModel.getGameId()
                : (currentGameId != null ? currentGameId : "Loading...");

        System.out.println(CYAN + "Game ID: "       + RESET + displayId
                + CYAN + "  |  Phase: " + RESET
                + (phase != null ? YELLOW + phase + RESET : "Starting..."));
        System.out.println(CYAN + "Active Player: " + RESET + BOLD
                + (currentP != null ? currentP : "---") + RESET);
        System.out.println();

        renderTurnOrder();
        System.out.println();
        renderOfferTrack();
        System.out.println();
        renderCards();
        System.out.println();
        renderPlayers();
        System.out.println();
        renderNotifications();
        System.out.println();
        renderCommands(phase);

        System.out.print("\n" + CYAN + "> " + RESET);
    }

    // -----------------------------------------------------------------------
    // Section renderers
    // -----------------------------------------------------------------------

    /**
     * Renders the turn-order tile, showing each slot with its occupant (if any),
     * food bonus, and PP malus. Data comes directly from
     * {@link GameModel#getTurnOrderSlots()}, which is kept up-to-date by
     * {@code TotemPlacedEvent} (clears slot) and {@code TotemReturnedEvent}
     * (fills slot).
     */
    private void renderTurnOrder() {
        System.out.println(YELLOW + BOLD + "=== TURN ORDER TILE ===" + RESET);

        List<TurnOrderSlotInfo> slots = gameModel.getTurnOrderSlots();
        if (slots.isEmpty()) {
            System.out.println("  (awaiting turn-order data...)");
            return;
        }

        for (int i = 0; i < slots.size(); i++) {
            TurnOrderSlotInfo slot = slots.get(i);
            String occupant = slot.occupantNickname();

            String bonusStr = GREEN
                    + " (food: " + (slot.foodBonus() >= 0 ? "+" : "") + slot.foodBonus() + ")"
                    + RESET;
            String malusStr = slot.prestigePointsMalus() != 0
                    ? RED + " (PP: " + slot.prestigePointsMalus() + ")" + RESET
                    : "";

            if (occupant == null) {
                System.out.printf("  Slot %d  [ empty ]%s%s%n", i + 1, bonusStr, malusStr);
            } else {
                boolean isMe = occupant.equals(gameModel.getMyNickname());
                String tag   = isMe ? YELLOW + BOLD + " ★ YOU" + RESET : "";
                System.out.printf("  Slot %d  %s%s%s%s%s%n",
                        i + 1, BOLD, occupant, RESET, tag, bonusStr + malusStr);
            }
        }
    }

    /**
     * Renders the offer track tiles, showing each tile's ID, food bonus,
     * pick limits, and current occupant.
     */
    private void renderOfferTrack() {
        System.out.println(PURPLE + BOLD + "=== OFFER TRACK ===" + RESET);
        for (OfferTileInfo tile : gameModel.getOfferTiles()) {
            String occupant = tile.occupantNickname() != null
                    ? YELLOW + "  <-- [ " + tile.occupantNickname() + " ]" + RESET
                    : "";
            System.out.printf("  [%c]  Food: %+d  |  Upper: %d  |  Lower: %d%s%n",
                    tile.tileID(), tile.foodBonus(),
                    tile.upperChoosable(), tile.lowerChoosable(),
                    occupant);
        }
    }

    /**
     * Renders the card rows (upper, lower) and the building market rows.
     */
    private void renderCards() {
        System.out.println(BLUE + BOLD + "--- CARDS IN PLAY ---" + RESET);

        System.out.println(WHITE + BOLD + "Upper Row:" + RESET);
        List<String> upper = gameModel.getUpperRow();
        if (upper.isEmpty()) {
            System.out.println("  (empty)");
        } else {
            upper.forEach(id -> System.out.println("  " + CardCatalog.getInstance().format(id)));
        }

        System.out.println(WHITE + BOLD + "\nLower Row:" + RESET);
        List<String> lower = gameModel.getLowerRow();
        if (lower.isEmpty()) {
            System.out.println("  (empty)");
        } else {
            lower.forEach(id -> System.out.println("  " + CardCatalog.getInstance().format(id)));
        }

        List<String> upperB = gameModel.getUpperRowBuildings();
        List<String> lowerB = gameModel.getLowerRowBuildings();
        if (!upperB.isEmpty() || !lowerB.isEmpty()) {
            System.out.println(CYAN + BOLD + "\nBuildings:" + RESET);
            upperB.forEach(id -> System.out.println("  [Up]  " + CardCatalog.getInstance().format(id)));
            lowerB.forEach(id -> System.out.println("  [Low] " + CardCatalog.getInstance().format(id)));
        }

        System.out.println("\nDeck remaining: " + YELLOW + gameModel.getDeckRemainingCount() + RESET);
    }

    /**
     * Renders every player's status: food, PP, characters, and buildings.
     * Displays cards for <em>all</em> players so a reconnecting user can
     * immediately see the full table state without any extra server round-trip.
     */
    private void renderPlayers() {
        System.out.println(PURPLE + BOLD + "=== PLAYERS STATUS ===" + RESET);

        for (String nickname : gameModel.getTurnOrder()) {
            int food   = gameModel.getFoodByPlayer().getOrDefault(nickname, 0);
            int pp     = gameModel.getPpByPlayer().getOrDefault(nickname, 0);

            int picksUp = gameModel.getRemainingUpper().getOrDefault(nickname, 0);
            int picksLow = gameModel.getRemainingLower().getOrDefault(nickname, 0);

            boolean isMe = nickname.equals(gameModel.getMyNickname());
            String marker = isMe ? YELLOW + BOLD + " (YOU)" + RESET : "";

            System.out.printf("  %-15s | Food: " + GREEN + "%2d" + RESET
                            + "  | PP: " + YELLOW + "%3d" + RESET
                            + "  | Picks (Up/Low): " + CYAN + "%d/%d" + RESET + "%s%n",
                    nickname, food, pp, picksUp, picksLow, marker);

            List<String> chars = gameModel.getCharactersByPlayer()
                    .getOrDefault(nickname, List.of());
            if (!chars.isEmpty()) {
                System.out.println("    Characters:");
                chars.forEach(id -> System.out.println(
                        "      - " + CardCatalog.getInstance().format(id)));
            }

            List<String> buildings = gameModel.getBuildingsByPlayer()
                    .getOrDefault(nickname, List.of());
            if (!buildings.isEmpty()) {
                System.out.println("    Buildings:");
                buildings.forEach(id -> System.out.println(
                        "      - " + CardCatalog.getInstance().format(id)));
            }

            System.out.println();
        }
    }

    /**
     * Renders the last {@value #MAX_NOTIFICATIONS} log entries.
     */
    private void renderNotifications() {
        System.out.println(WHITE + BOLD + "--- RECENT LOGS ---" + RESET);
        if (notifications.isEmpty()) {
            System.out.println("  No recent activity.");
        } else {
            notifications.forEach(n -> System.out.println("  " + n));
        }
    }

    /**
     * Retrieves the full description of a card from the catalog and displays it.
     *
     * @param cardId the ID of the card to lookup (e.g., "C_008")
     */
    public void onShowCardInfo(String cardId) {
        // Fetch the detailed description
        String fullInfo = CardCatalog.getInstance().getFullDescription(cardId);

        // Split the info by newlines to add them cleanly to the notification buffer
        String[] lines = fullInfo.split("\n");
        for (String line : lines) {
            // We use standard white text for the info output, or you can add color codes
            addNotification(WHITE + line + RESET);
        }
    }

    /**
     * Renders the context-sensitive command list.
     * Shows a waiting message when it is not this client's turn.
     *
     * @param phase the current FSM phase; if {@code null} the section is skipped
     */
    private void renderCommands(PhaseType phase) {
        if (phase == null) return;

        String myNick   = gameModel.getMyNickname();
        String currentP = gameModel.getCurrentPlayer();

        if (myNick == null || currentP == null) {
            System.out.println("  (Loading player data...)");
            return;
        }

        System.out.println(GREEN + BOLD + "--- AVAILABLE COMMANDS ---" + RESET);

        if (!myNick.equals(currentP)) {
            System.out.println("  Waiting for " + BOLD + currentP + RESET + " to finish their turn...");
            return;
        }

        switch (phase) {
            case TOTEM_PLACEMENT -> {
                System.out.println("  move <tileID>     — Place your totem on a free offer tile  (e.g., move B)");
                System.out.println("  info <cardID>     — View full details of a specific card   (e.g., info C_012)"); // Added
            }

            case ACTION_RESOLUTION -> {
                System.out.println("  resolve <id...>   — Pick card IDs from the board  (e.g., resolve C_001 E_002)");
                System.out.println("  move T            — Return your totem and END YOUR TURN");
                System.out.println("  info <cardID>     — View full details of a specific card   (e.g., info C_012)"); // Added
                System.out.println(YELLOW + "  (You MUST type 'move T' after resolving actions.)" + RESET);
            }

            default -> {
                System.out.println("  info <cardID>     — View full details of a specific card   (e.g., info C_012)"); // Added
                System.out.println("  (Waiting for the current phase to complete...)");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /** Prints the game title banner. */
    private void printHeader() {
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println(CYAN + BOLD + "          MESOS — PREHISTORIC           " + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
    }

    /** Clears the terminal using ANSI escape codes. */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}