package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.view.CardCatalog;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.AbstractClientView;
import it.polimi.ingsw.am02.common.ProjectInfo;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 *
 * <h2>Thread model</h2>
 * <p>All public callbacks enqueue a {@link Runnable} onto {@link #eventQueue}.
 * A single background thread ({@code TUI-Event-Processor}) drains the queue
 * sequentially, guaranteeing that terminal writes never interleave.</p>
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

    /** Single-writer queue that serializes all terminal-output tasks. */
    private final BlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<>();

    private boolean bannerPrinted = false;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a new {@code TuiView} bound to the given lobby model and starts
     * the background event-processor thread.
     *
     * @param lobbyModel the client-side lobby model; must not be {@code null}
     */
    public TuiView(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
        startEventProcessor();
    }

    /**
     * Starts the background event-processor thread that serializes all UI
     * updates through {@link #eventQueue}, preventing concurrent terminal writes.
     * A 50 ms pacing delay is applied after each task to smooth catch-up replay.
     */
    private void startEventProcessor() {
        Thread processor = new Thread(() -> {
            try {
                while (true) {
                    Runnable event = eventQueue.take();
                    event.run();
                    System.out.flush();
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "TUI-Event-Processor");
        processor.setDaemon(true);
        processor.start();
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
        eventQueue.add(() -> {
            this.gameModel = gameModel;
            if (this.gameModel != null) {
                renderFullGame();
            }
        });
    }

    /** Pending summary-card text to overlay on the next repaint cycle. */
    private String pendingSummary = null;

    // -----------------------------------------------------------------------
    // Notification helper
    // -----------------------------------------------------------------------

    /**
     * Appends a message to the notification ring buffer and triggers a repaint.
     * If {@link #gameModel} is not yet available, the message is printed inline
     * without a full repaint.
     *
     * @param message ANSI-formatted log line to display
     */
    private void addNotification(String message) {
        notifications.addLast(message);
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.removeFirst();
        }

        if (gameModel != null && gameModel.getGameId() != null) {
            renderFullGame();
        } else {
            System.out.println(message);
            System.out.print("\n" + CYAN + "> " + RESET);
        }
    }

    // -----------------------------------------------------------------------
    // LOBBY CALLBACKS
    // -----------------------------------------------------------------------

    /**
     * Called when the server responds to a nickname registration attempt.
     * Prints a confirmation or rejection message and refreshes the prompt.
     *
     * @param username the requested nickname
     * @param accepted {@code true} if the server accepted the nickname
     * @param reason   human-readable rejection reason; ignored when {@code accepted} is {@code true}
     */
    @Override
    public void onUsernameResult(String username, boolean accepted, String reason) {
        eventQueue.add(() -> {
            if (accepted) {
                clearScreen();
                printHeader();
                System.out.println(GREEN + "Successfully logged in as: " + BOLD + username + RESET);
                System.out.println("\nCommands: create <size> | join <index> | quit");
            } else {
                System.out.println(RED + "[ERROR] Username '" + username + "' rejected: " + reason + RESET);
            }
            System.out.print("\n" + CYAN + "> " + RESET);
        });
    }

    /**
     * Redraws the pre-lobby screen with the current list of open lobbies.
     * No-op when a game is already in progress.
     *
     * @param lobbies snapshot of lobbies currently waiting for players
     */
    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        eventQueue.add(() -> {
            if (gameModel != null) return;
            clearScreen();
            if (!bannerPrinted) {
                printBanner();
                bannerPrinted = true;
            }
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
        });
    }

    /**
     * Redraws the lobby screen with the updated player list and totem selections.
     * Adapts the command hints based on whether the local player has already set
     * a nickname and/or picked a totem. No-op when a game is already in progress.
     *
     * @param lobby the updated lobby snapshot
     */
    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        eventQueue.add(() -> {
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

            List<Totem> availableTotems = new ArrayList<>(Arrays.asList(Totem.values()));
            availableTotems.removeAll(lobby.chosenTotems().values());

            System.out.println("\n" + CYAN + "Available totems: " + RESET + availableTotems);

            String myNick = lobbyModel.getMyNickname();

            if (myNick == null || myNick.isBlank()) {
                System.out.println("\n" + YELLOW + BOLD + ">> STEP 1: Enter a nickname to join" + RESET);
                System.out.println("Commands: nick <name> | leave | quit");
            } else {
                boolean hasTotem = lobby.chosenTotems().containsKey(myNick);
                String totemCmd = hasTotem ? "totem <color> (to change)" : "totem <color>";

                Totem myTotem = lobby.chosenTotems().get(myNick);
                String statusMsg = hasTotem
                        ? "You are ready! Totem: " + totemColor(myTotem) + "[" + myTotem + "]" + RESET
                        : "Pick your totem!";
                System.out.println("\n" + GREEN + BOLD + ">> STEP 2: Nickname set (" + myNick + "). " + statusMsg + RESET);

                System.out.println("Commands: nick <name> (to change) | " + totemCmd + " | totems | leave | quit");
            }

            System.out.print("\n" + CYAN + "> " + RESET);
        });
    }

    /**
     * Appends a notification listing the totems not yet claimed in the current lobby.
     */
    @Override
    public void onShowAvailableTotems() {
        eventQueue.add(() -> {
            LobbyInfo lobby = lobbyModel.getCurrentLobby();
            if (lobby != null) {
                List<Totem> available = new ArrayList<>(Arrays.asList(Totem.values()));
                available.removeAll(lobby.chosenTotems().values());

                addNotification(CYAN + "[LOBBY] Available totems: " + GREEN + available + RESET);
            }
        });
    }

    /**
     * Notifies the user that the lobby they were in has been dissolved by the server.
     */
    @Override
    public void onLobbyDissolved() {
        eventQueue.add(() -> {
            System.out.println("\n" + RED + "[LOBBY] The lobby has been dissolved." + RESET);
            System.out.print("\n" + CYAN + "> " + RESET);
        });
    }

    // -----------------------------------------------------------------------
    // GAME CALLBACKS
    // -----------------------------------------------------------------------

    /**
     * Stores the assigned game ID, detaches the view from the lobby-model
     * observer chain, and posts a game-started notification.
     *
     * @param gameId the server-assigned game identifier
     */
    @Override
    public void onGameStarted(String gameId) {
        eventQueue.add(() -> {
            this.currentGameId = gameId;
            lobbyModel.removeObserver(this);
            addNotification(GREEN + BOLD + "GAME STARTED! ID: " + gameId + RESET);
        });
    }

    /**
     * Triggers a full repaint once the initial board configuration is available.
     *
     * @param turnOrder    initial turn order (top to bottom)
     * @param initialFood  initial food allocation per player
     * @param board        initial board snapshot
     */
    @Override
    public void onGameSetupCompleted(List<String> turnOrder,
                                     Map<String, Integer> initialFood,
                                     BoardSnapshot board) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Triggers a full repaint when the FSM transitions to a new phase.
     *
     * @param phase         the new FSM phase
     * @param currentPlayer nickname of the player who acts in this phase
     * @param order         current turn order
     */
    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> order) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Triggers a full repaint when control passes to a different player.
     *
     * @param nextPlayer the nickname of the player who is now active
     */
    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Triggers a full repaint when a new turn order is established.
     *
     * @param turnOrder the updated turn-order list; the first element acts first
     */
    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Appends a notification when a player places their totem on the offer track.
     *
     * @param nickname the player who placed the totem
     * @param tileID   identifier of the tile where the totem was placed
     */
    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        eventQueue.add(() -> addNotification(WHITE + "[BOARD] " + nickname
                + " placed their totem on tile " + BOLD + tileID + RESET));
    }

    /**
     * Appends a notification when a player's totem returns to the turn-order tile.
     *
     * @param nickname          the player whose totem returned
     * @param turnOrderPosition zero-based slot index on the turn-order tile;
     *                          displayed as one-based for readability
     */
    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        eventQueue.add(() -> addNotification(WHITE + "[BOARD] " + nickname
                + " returned to turn-order slot " + (turnOrderPosition + 1) + RESET));
    }

    /**
     * Triggers a full repaint when the offer-track tile state changes.
     *
     * @param tiles updated snapshot of all offer tiles
     */
    @Override
    public void onOfferTilesUpdated(List<OfferTileInfo> tiles) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Triggers a full repaint when the card rows or deck size change.
     *
     * @param newUpper  IDs of cards now in the upper row
     * @param newLower  IDs of cards now in the lower row
     * @param deckCount remaining cards in the draw pile
     */
    @Override
    public void onBoardUpdated(List<String> newUpper, List<String> newLower, int deckCount) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Posts an era-change notification and immediately repaints the board
     * to reflect the new building market.
     *
     * @param era               the era that has just begun
     * @param newUpperBuildings building card IDs now in the upper row
     * @param newLowerBuildings building card IDs now in the lower row
     */
    @Override
    public void onEraChanged(Era era, List<String> newUpperBuildings, List<String> newLowerBuildings) {
        eventQueue.add(() -> {
            addNotification(PURPLE + BOLD + "[ERA] Era " + era + " has begun!" + RESET);
            renderFullGame();
        });
    }

    /**
     * Triggers a full repaint when a player's initial pick limits are set.
     *
     * @param nickname the player whose limits were initialized
     * @param upper    number of cards choosable from the upper row
     * @param lower    number of cards choosable from the lower row
     */
    @Override
    public void onPlayerLimitsInitialized(String nickname, int upper, int lower) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Triggers a full repaint when a player's remaining pick limits change.
     *
     * @param nickname the player whose limits changed
     * @param upper    updated upper-row picks remaining
     * @param lower    updated lower-row picks remaining
     */
    @Override
    public void onPlayerLimitsUpdated(String nickname, int upper, int lower) {
        eventQueue.add(this::renderFullGame);
    }

    /**
     * Appends a resource-change notification for the given player.
     *
     * @param nickname  the affected player
     * @param resource  the resource type that changed
     * @param newValue  the player's new resource total
     */
    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
        eventQueue.add(() -> addNotification(YELLOW + "[RESOURCE] " + nickname
                + " now has " + newValue + " " + resource + RESET));
    }

    /**
     * Appends a card-pickup notification, formatting the card ID for readability.
     *
     * @param nickname  the player who took the card
     * @param cardID    identifier of the card taken
     * @param cardType  whether the card is a character or a building
     * @param sourceRow the row the card was taken from
     */
    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        eventQueue.add(() -> {
            String formatted = CardCatalog.getInstance().format(cardID);
            addNotification(CYAN + "[ACTION] " + nickname + " took " + formatted + RESET);
        });
    }

    /**
     * Appends a notification when a round or final event is resolved.
     *
     * @param eventID   identifier of the resolved event card
     * @param eventName human-readable name of the event
     */
    @Override
    public void onEventResolved(String eventID, String eventName) {
        eventQueue.add(() -> addNotification(PURPLE + "[EVENT] Resolved: " + eventName + RESET));
    }

    /**
     * Appends a notification when a player gains an extra turn,
     * reporting their remaining pick budget.
     *
     * @param nickname       the player who gained the extra turn
     * @param remainingUpper upper-row picks still available
     * @param remainingLower lower-row picks still available
     */
    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        eventQueue.add(() -> addNotification(GREEN + "[EXTRA] " + nickname + " gained an extra turn!"
                + " (Up:" + remainingUpper + " Lw:" + remainingLower + ")" + RESET));
    }

    /**
     * Appends a notification when a player's extra turn concludes.
     *
     * @param nickname the player whose extra turn has ended
     */
    @Override
    public void onExtraTurnEnded(String nickname) {
        eventQueue.add(() -> addNotification(YELLOW + "[EXTRA] " + nickname + "'s extra turn ended." + RESET));
    }

    /**
     * Appends a disconnection alert for the given player.
     *
     * @param nickname the player who disconnected
     */
    @Override
    public void onPlayerDisconnected(String nickname) {
        eventQueue.add(() -> addNotification(RED + BOLD + "[!] Player disconnected: " + nickname + RESET));
    }

    /**
     * Appends a reconnection notification for the given player.
     *
     * @param nickname the player who reconnected
     */
    @Override
    public void onPlayerReconnected(String nickname) {
        eventQueue.add(() -> addNotification(GREEN + "[!] Player reconnected: " + nickname + RESET));
    }

    /**
     * Appends an error message to the notification log.
     *
     * @param message the error description to display
     */
    @Override
    public void onError(String message) {
        eventQueue.add(() -> addNotification(RED + "[ERROR] " + message + RESET));
    }

    /**
     * Clears the screen and displays a connection-lost banner.
     * If the game ID is known, it is shown so the player can reconnect later.
     */
    @Override
    public void onConnectionLost() {
        eventQueue.add(() -> {
            clearScreen();
            printHeader();
            System.out.println(RED + BOLD + "=== SERVER CONNECTION LOST ===" + RESET);
            System.out.println(YELLOW + "The server is currently offline or unreachable." + RESET);
            System.out.println("Please wait. The client will attempt to reconnect automatically...\n");

            String idToPrint = (gameModel != null && gameModel.getGameId() != null)
                    ? gameModel.getGameId()
                    : currentGameId;

            if (idToPrint != null) {
                System.out.println(CYAN + "Your Game ID (in case you need to reconnect later): " + BOLD + idToPrint + RESET);
            }
        });
    }

    /**
     * Clears the screen and displays a reconnection-success banner.
     * The controller will follow up by replaying the game event history.
     */
    @Override
    public void onConnectionRestored() {
        eventQueue.add(() -> {
            clearScreen();
            printHeader();
            System.out.println(GREEN + BOLD + "=== CONNECTION RESTORED ===" + RESET);
            System.out.println("Successfully reconnected to the server!");
            System.out.println("Resynchronizing state, please wait...\n");
        });
    }

    /**
     * Appends a notification that the AutoPlayer countdown has begun for a
     * disconnected player.
     *
     * @param nickname the disconnected player
     * @param seconds  seconds remaining before the AutoPlayer takes over
     */
    @Override
    public void onAutoPlayerTimerStarted(String nickname, long seconds) {
        eventQueue.add(() -> addNotification(YELLOW + "[BOT] " + nickname
                + " is disconnected — AutoPlayer takes over in " + seconds + "s." + RESET));
    }

    /**
     * Appends a notification that the AutoPlayer is acting on behalf of a
     * disconnected player.
     *
     * @param nickname the player being substituted by the AutoPlayer
     */
    @Override
    public void onAutoPlayerInvoked(String nickname) {
        eventQueue.add(() -> addNotification(PURPLE + BOLD + "[BOT] AutoPlayer acting for "
                + nickname + "..." + RESET));
    }

    /**
     * Clears the game model and displays the end-of-game screen with the
     * winner(s) and complete final rankings.
     *
     * @param winners       nicknames of the winning player(s)
     * @param finalRankings full ranked list with each player's final PP total
     */
    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        eventQueue.add(() -> {
            this.gameModel = null;
            clearScreen();
            printHeader();
            System.out.println(GREEN + BOLD + "=== GAME OVER ===" + RESET);
            System.out.println((winners.size() == 1 ? "Winner: " : "Winners: ") + String.join(", ", winners));
            System.out.println("\n" + PURPLE + "--- FINAL RANKINGS ---" + RESET);
            for (int i = 0; i < finalRankings.size(); i++) {
                PlayerFinalScore s = finalRankings.get(i);
                System.out.printf("  %d. %-15s  PP: %d%n",
                        i + 1, s.nickname(), s.totalPrestigePoints());
            }
            System.out.println("\nType 'lobby' to return to lobby, or 'quit' to exit.");
            System.out.print("\n" + CYAN + "> " + RESET);
        });
    }

    /**
     * Clears the game model and displays an abort screen indicating the
     * player who won by forfeit due to all others disconnecting.
     *
     * @param lastManStanding the only player who remained connected
     */
    @Override
    public void onGameAborted(String lastManStanding) {
        eventQueue.add(() -> {
            this.gameModel = null;
            clearScreen();
            printHeader();
            System.out.println(RED + BOLD + "=== GAME ABORTED ===" + RESET);
            System.out.println("Winner by forfeit: " + lastManStanding);
            System.out.println("\nType 'lobby' to return to lobby, or 'quit' to exit.");
            System.out.print("\n" + CYAN + "> " + RESET);
        });
    }

    // onGameRecoveryFailed is intentionally not overridden: it is a defensive
    // server-side fallback (see GameController) that only fires when the global
    // timer expires with no active player — a "should never happen" terminal
    // state in which no client is connected to display anything. The TUI
    // inherits the no-op default from AbstractClientView. The full pipeline is
    // kept for a future view that may want to surface this state.

    /**
     * Appends a notification that the global forfeit countdown has started.
     * Only the sole remaining active player receives this event.
     *
     * @param seconds countdown duration in seconds
     */
    @Override
    public void onGlobalTimerStarted(long seconds) {
        // "about Ns": the server sends this at the end of the catch-up replay, so a
        // precise figure would already be stale by the time it is shown. Uses the
        // server-supplied seconds so it tracks any future timeout change.
        eventQueue.add(() -> addNotification(RED + BOLD + "[!] You are the only active player. "
                + "If no one reconnects within about " + seconds + "s, you win by forfeit." + RESET));
    }

    /**
     * Appends a notification that the global forfeit countdown has been canceled
     * because another player has reconnected.
     */
    @Override
    public void onGlobalTimerCancelled() {
        eventQueue.add(() -> addNotification(GREEN + BOLD + "[!] A player has reconnected. "
                + "The forfeit countdown has been cancelled." + RESET));
    }

    /**
     * Resets all in-game state (game model, game ID, notification buffer) and
     * clears the screen in preparation for the lobby view.
     */
    @Override
    public void onReturnToLobby() {
        eventQueue.add(() -> {
            this.currentGameId = null;
            this.gameModel = null;
            this.notifications.clear();

            clearScreen();
            printHeader();
        });
    }

    /**
     * Appends the full description of the specified card to the notification log.
     *
     * @param cardId the card identifier to look up in the {@link CardCatalog}
     */
    public void onShowCardInfo(String cardId) {
        eventQueue.add(() -> {
            String fullInfo = CardCatalog.getInstance().getFullDescription(cardId);
            String[] lines = fullInfo.split("\n");
            for (String line : lines) {
                addNotification(WHITE + line + RESET);
            }
        });
    }

    /**
     * Schedules a summary-card overlay on the next repaint.
     * The overlay replaces the notification log for one render cycle and is
     * then cleared automatically.
     */
    public void onShowSummaryCard() {
        eventQueue.add(() -> {
            this.pendingSummary = CardCatalog.getInstance().getSummaryCardText();
            renderFullGame();
        });
    }

    /**
     * Prints a context-sensitive command cheatsheet to standard output.
     *
     * @param inPreLobby {@code true} when the player has not yet joined any lobby
     * @param inLobby    {@code true} when the player is waiting in a lobby
     * @param inGame     {@code true} when a game is in progress
     */
    public void onShowHelp(boolean inPreLobby, boolean inLobby, boolean inGame) {
        eventQueue.add(() -> {
            System.out.println("\n" + CYAN + BOLD + "--- COMMAND CHEATSHEET ---" + RESET);
            if (inPreLobby) {
                System.out.println("  create <size>         - Start a new lobby (size 2-5)");
                System.out.println("  join <index>          - Join a waiting lobby");
                System.out.println("  reconnect <nick> <id> - Rejoin a crashed game");
            } else if (inLobby) {
                System.out.println("  nick <name>           - Set or change your nickname");
                System.out.println("  totem <color>         - Pick your totem (WHITE, PURPLE, BLUE, RED, YELLOW)");
                System.out.println("  totems                - View available totems");
                System.out.println("  leave                 - Exit the lobby");
            } else if (inGame) {
                System.out.println("  move <tileID>         - Place totem on the offer track (e.g., move B)");
                System.out.println("  resolve <id...>       - Take specific cards (e.g., resolve C_001 E_002)");
                System.out.println("  move T                - Return totem and END YOUR TURN");
                System.out.println("  info <cardID>         - Read full card details (e.g., info B_004)");
                System.out.println("  summary               - Show the summary card (quick-reference rules)");
                System.out.println("  lobby                 - Leave the game and return to lobby");
            }
            System.out.println("  quit                  - Close the application");

            if (inGame && gameModel != null && gameModel.getCurrentPlayer() != null) {
                if (gameModel.getCurrentPlayer().equals(gameModel.getMyNickname())) {
                    System.out.print("\n" + GREEN + BOLD + "[YOUR TURN] > " + RESET);
                } else {
                    System.out.print("\n" + YELLOW + "[Waiting for " + gameModel.getCurrentPlayer() + "...] > " + RESET);
                }
            } else {
                System.out.print("\n" + CYAN + "> " + RESET);
            }
        });
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
                + (phase != null ? YELLOW + phase + RESET : "Starting...")
                + CYAN + "  |  Era: " + RESET + PURPLE + BOLD
                + gameModel.getCurrentEra() + RESET);
        System.out.println(CYAN + "Active Player: " + RESET + BOLD
                + (currentP != null ? currentP : "---") + RESET);
        System.out.println();

        renderTurnOrder();
        System.out.println();
        renderOfferTrack();
        System.out.println();
        renderCards();
        System.out.println();
        renderReserve();
        System.out.println();
        renderPlayers();
        System.out.println();
        renderNotifications();
        System.out.println();
        renderCommands(phase);

        if (currentP != null && currentP.equals(gameModel.getMyNickname())) {
            System.out.print("\n" + GREEN + BOLD + "[YOUR TURN] > " + RESET);
        } else if (currentP != null) {
            System.out.print("\n" + YELLOW + "[Waiting for " + currentP + "...] > " + RESET);
        } else {
            System.out.print("\n" + CYAN + "> " + RESET);
        }
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
            String bonusStr = GREEN + " (food: "
                    + (slot.foodBonus() >= 0 ? "+" : "") + slot.foodBonus() + ")" + RESET;
            String malusStr = slot.prestigePointsMalus() != 0
                    ? RED + " (PP: " + slot.prestigePointsMalus() + ")" + RESET : "";
            if (occupant == null) {
                System.out.printf("  Slot %d  [ empty ]%s%s%n", i + 1, bonusStr, malusStr);
            } else {
                boolean isMe = occupant.equals(gameModel.getMyNickname());
                String tag   = isMe ? YELLOW + BOLD + " ★ YOU" + RESET : "";
                System.out.printf("  Slot %d  %s%s%s%s%n",
                        i + 1, BOLD + formatOccupant(occupant) + RESET, tag, bonusStr, malusStr);
            }
        }
    }

    /**
     * Renders the offer track tiles, showing each tile's ID, food bonus,
     * pick limits, and current occupant (if any).
     */
    private void renderOfferTrack() {
        System.out.println(PURPLE + BOLD + "=== OFFER TRACK ===" + RESET);
        for (OfferTileInfo tile : gameModel.getOfferTiles()) {
            String occupant = tile.occupantNickname() != null
                    ? YELLOW + "  <-- " + formatOccupant(tile.occupantNickname()) + RESET
                    : "";
            System.out.printf("  [%c]  Food: %+d  |  Upper: %d  |  Lower: %d%s%n",
                    tile.tileID(), tile.foodBonus(),
                    tile.upperChoosable(), tile.lowerChoosable(), occupant);
        }
    }

    /**
     * Renders the card rows (upper and lower) and the building market rows,
     * including the remaining deck count.
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
     * Renders the common reserve. Mirrors the physical game's shared supply of
     * food and prestige tokens, which is <em>unlimited</em>: there is no
     * server-side count to display, so each pile is shown as a fixed mound of
     * glyphs annotated with an infinity symbol.
     */
    private void renderReserve() {
        System.out.println(CYAN + BOLD + "=== COMMON RESERVE ===" + RESET);
        System.out.println("  Food     " + GREEN + "●●●●●" + RESET
                + "  (" + GREEN + "∞" + RESET + " unlimited supply)");
        System.out.println("  Prestige " + YELLOW + "◆◆◆◆◆" + RESET
                + "  (" + YELLOW + "∞" + RESET + " unlimited supply)");
    }

    /**
     * Renders every player's status: food, PP, pick limits, characters, and
     * buildings. Displays cards for <em>all</em> players so a reconnecting user
     * can immediately see the full table state without any extra server round-trip.
     */
    private void renderPlayers() {
        System.out.println(PURPLE + BOLD + "=== PLAYERS STATUS ===" + RESET);

        for (String nickname : gameModel.getTurnOrder()) {
            int food   = gameModel.getFoodByPlayer().getOrDefault(nickname, 0);
            int pp     = gameModel.getPpByPlayer().getOrDefault(nickname, 0);

            int picksUp = gameModel.getRemainingUpper().getOrDefault(nickname, 0);
            int picksLow = gameModel.getRemainingLower().getOrDefault(nickname, 0);

            boolean isMe = nickname.equals(gameModel.getMyNickname());
            String prefix = isMe ? GREEN + BOLD + "=> " + RESET : "   ";
            String marker = isMe ? YELLOW + BOLD + " (YOU)" + RESET : RESET;

            Totem t = gameModel.getTotem(nickname);
            String totemTag = t != null ? totemColor(t) + "[" + t + "]" + RESET + " " : "";
            System.out.printf("%s%s%-15s | Food: " + GREEN + "%2d" + RESET
                            + "  | PP: " + YELLOW + "%3d" + RESET
                            + "  | Picks (Up/Low): " + CYAN + "%d/%d" + RESET + "%s%n",
                    prefix, totemTag, nickname, food, pp, picksUp, picksLow, marker);

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
     * Formats a player's totem color tag and nickname for terminal display.
     *
     * @param nickname the player's nickname
     * @return ANSI-colored {@code [TOTEM] nickname} string, or just the
     *         nickname if the totem is unknown
     */
    private String formatOccupant(String nickname) {
        Totem totem = gameModel.getTotem(nickname);
        String totemStr = totem != null ? totemColor(totem) + "[" + totem + "]" + RESET + " " : "";
        return totemStr + nickname;
    }

    /**
     * Maps a {@link Totem} enum value to its corresponding ANSI color code.
     *
     * @param totem the totem; must not be {@code null}
     * @return ANSI escape sequence for the totem's color
     */
    private String totemColor(Totem totem) {
        return switch (totem) {
            case WHITE  -> WHITE;
            case PURPLE -> PURPLE;
            case BLUE   -> BLUE;
            case RED    -> RED;
            case YELLOW -> YELLOW;
        };
    }

    /**
     * Renders the last {@value #MAX_NOTIFICATIONS} log entries, or — if a
     * summary-card overlay is pending — displays that instead for one cycle.
     */
    private void renderNotifications() {
        if (pendingSummary != null) {
            System.out.println(WHITE + BOLD + "--- SUMMARY CARD ---" + RESET);
            System.out.println(CYAN + pendingSummary + RESET);
            pendingSummary = null;
        } else {
            System.out.println(WHITE + BOLD + "--- RECENT LOGS ---" + RESET);
            if (notifications.isEmpty()) {
                System.out.println("  No recent activity.");
            } else {
                notifications.forEach(n -> System.out.println("  " + n));
            }
        }
    }

    /**
     * Renders the context-sensitive command list, adapting it to the current FSM
     * phase and whether it is the local player's turn.
     *
     * @param phase the current FSM phase; the section is skipped if {@code null}
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
        } else {
            switch (phase) {
                case TOTEM_PLACEMENT -> System.out.println("  move <tileID>     — Place your totem on a free offer tile  (e.g., move B)");

                case ACTION_RESOLUTION -> {
                    System.out.println("  resolve <id...>   — Pick card IDs from the board  (e.g., resolve C_001 E_002)");
                    System.out.println("  move T            — Return your totem and END YOUR TURN");
                    System.out.println(YELLOW + "  (You MUST type 'move T' after resolving actions.)" + RESET);
                }

                default -> System.out.println("  (Waiting for the current phase to complete...)");
            }
        }

        System.out.println("  info <cardID>     — View full details of a specific card   (e.g., info C_012)");
        System.out.println("  summary           — Show the summary card (quick-reference rules)");
        System.out.println("  lobby             — Leave the game and return to lobby");
        System.out.println("  quit              — Close the application");
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /** Prints the full startup banner with team info, only shown once per session. */
    private void printBanner() {
        System.out.println(GREEN + BOLD
                + "  /\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\\n"
                + "  \\                                                /\n"
                + "  /    M  E  S  O  S  —  P R E H I S T O R I C   \\\n"
                + "  \\                                                /\n"
                + "  /\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\"
                + RESET);
        System.out.println(YELLOW
                + "\n  " + ProjectInfo.GROUP
                + "  │  " + ProjectInfo.YEAR
                + "  │  " + ProjectInfo.UNIVERSITY
                + RESET);
        System.out.println(WHITE
                + "  ──────────────────────────────────────────────────────"
                + RESET);
        System.out.println(PURPLE + "  Members:" + RESET);
        ProjectInfo.MEMBERS.forEach((name, code) ->
                System.out.println("  " + YELLOW + "◆" + RESET
                        + "  " + String.format("%-20s", name)
                        + " [" + code + "]"));
        System.out.println(WHITE
                + "  ──────────────────────────────────────────────────────"
                + RESET);
        System.out.println(WHITE
                + "  " + ProjectInfo.COURSE
                + "  │  " + ProjectInfo.PROFESSOR
                + "\n"
                + RESET);
    }

    /** Prints the compact game-title header shown at the top of every screen. */
    private void printHeader() {
        System.out.println(GREEN + BOLD
                + "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "      M  E  S  O  S   —   P  R  E  H  I  S  T  O  R  I  C\n"
                + "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
                + RESET);
    }

    /** Clears the terminal using ANSI escape codes. */
    private void clearScreen() {
        System.out.print("\n\033[H\033[2J");
        System.out.flush();
    }
}