package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.AbstractClientView;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;
import java.util.Map;

/**
 * Textual user interface. Registered as observer on both {@link LobbyModel}
 * and {@link GameModel}. Receives granular push updates and renders only
 * the affected section, falling back to a full re-render on structural changes.
 */
public class TuiView extends AbstractClientView {

    private final LobbyModel lobbyModel;
    private GameModel gameModel; // null until GameStartedEvent

    public TuiView(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
        lobbyModel.addObserver(this);
    }



    @Override
    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
        gameModel.addObserver(this);
    }

    // LOBBY CALLBACKS

    @Override
    public void onUsernameResult(String username, boolean accepted, String reason) {
        if (accepted) {
            System.out.println("\n[SUCCESS] Nickname set to: " + username);
        } else {
            System.err.println("\n[ERROR] Nickname '" + username + "' is not available: " + reason);
        }
        System.out.print("\n> ");
    }

    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        clearScreen();
        printHeader();
        System.out.println("Status: Anonymous");
        System.out.println("\n--- AVAILABLE LOBBIES ---");
        if (lobbies.isEmpty()) {
            System.out.println("No active lobbies. Create one with: create <size>");
        } else {
            for (int i = 0; i < lobbies.size(); i++) {
                LobbyInfo info = lobbies.get(i);
                System.out.printf("  [%d] Players: %d/%d%n",
                        i, info.currentPlayers().size(), info.expectedPlayers());
            }
        }
        System.out.println("\nCommands: create <size> | join <index> | reconnect <gameId> <nickname> | quit");
        System.out.print("\n> ");
    }

    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        clearScreen();
        printHeader();
        String myNick = lobbyModel.getMyNickname();
        System.out.println("Status: " + (myNick != null ? myNick : "Anonymous (Set a nick!)"));
        System.out.println("\n--- LOBBY: " + lobby.lobbyId() + " ---");
        System.out.printf("Players: %d/%d%n",
                lobby.currentPlayers().size(), lobby.expectedPlayers());

        lobby.currentPlayers().forEach(n -> {
            Totem chosen = lobby.chosenTotems().get(n);
            String totemStr = (chosen != null) ? " [" + chosen + "]" : " [no totem]";
            // If the player hasn't set a nickname yet, the server might send a placeholder like "Unknown" or the clientId.
            System.out.println("  - " + n + totemStr);
        });

        System.out.println("\nCommands: nick <name> | totem <color> | leave | quit");
        System.out.print("\n> ");
    }

    @Override
    public void onLobbyDissolved() {
        System.out.println("\n[LOBBY] The lobby has been dissolved.");
        System.out.print("\n> ");
    }

    private String lastNotification = "";

    // ==================== GAME CALLBACKS ====================

    @Override
    public void onGameStarted(String gameId) {
        lastNotification = "Game started! ID: " + gameId;
        renderFullGame();
    }

    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {
        lastNotification = "Game setup completed.";
        renderFullGame();
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        lastNotification = "Phase changed to " + phase;
        renderFullGame();
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        lastNotification = "It is now " + nextPlayer + "'s turn.";
        renderFullGame();
    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        lastNotification = nickname + " placed totem on tile " + tileID;
        renderFullGame();
    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        lastNotification = nickname + " returned totem to slot " + turnOrderPosition;
        renderFullGame();
    }

    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) {
        lastNotification = "Board updated.";
        renderFullGame();
    }

    @Override
    public void onEraChanged(List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {
        lastNotification = "A new era has begun!";
        renderFullGame();
    }

    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
        lastNotification = nickname + " now has " + newValue + " " + resource;
        renderFullGame();
    }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        lastNotification = nickname + " took " + cardType + " '" + cardID + "' from " + sourceRow + " row";
        renderFullGame();
    }

    @Override
    public void onEventResolved(String eventName) {
        lastNotification = "Event resolved: " + eventName;
        renderFullGame();
    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        lastNotification = nickname + " gained an extra turn!";
        renderFullGame();
    }

    @Override
    public void onExtraTurnEnded(String nickname) {
        lastNotification = nickname + "'s extra turn ended.";
        renderFullGame();
    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        clearScreen();
        printHeader();
        System.out.println("=== GAME OVER ===");
        System.out.println("Winners: " + String.join(", ", winners));
        System.out.println("\n--- FINAL RANKINGS ---");
        for (int i = 0; i < finalRankings.size(); i++) {
            PlayerFinalScore s = finalRankings.get(i);
            System.out.printf("  %d. %-15s  PP: %d%n", i + 1, s.nickname(), s.totalPrestigePoints());
        }
        System.out.println("\nType quit to exit.");
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        lastNotification = "[!] Player disconnected: " + nickname;
        renderFullGame();
    }

    @Override
    public void onError(String message) {
        lastNotification = "[ERROR] " + message;
        renderFullGame();
    }

    // ==================== FULL RE-RENDER ====================

    private void renderFullGame() {
        if (gameModel == null) return;
        clearScreen();
        printHeader();

        System.out.println(">>> LATEST UPDATE: " + lastNotification);
        System.out.println("----------------------------------------");

        PhaseType phase = gameModel.getCurrentPhase();
        System.out.println("Game: " + gameModel.getGameId());
        System.out.println("Phase: " + (phase != null ? phase : "starting..."));
        System.out.println("Current player: " + gameModel.getCurrentPlayer());
        System.out.println();

        renderBoard();
        System.out.println();
        renderPlayers(); // This ALREADY prints cards, food, and PP for every player!
        System.out.println();
        renderCommands(phase);

        System.out.print("\n> ");
    }

    private void renderBoard() {
        System.out.println("--- OFFER TRACK ---");
        for (OfferTileInfo tile : gameModel.getOfferTiles()) {
            String occupant = tile.occupantNickname() != null
                    ? " <- " + tile.occupantNickname() : "";
            System.out.printf("  [%c] food:+%d  upper:%d  lower:%d%s%n",
                    tile.tileID(), tile.foodBonus(),
                    tile.upperChoosable(), tile.lowerChoosable(), occupant);
        }
        System.out.println();
        System.out.println("Upper row (cards): " + formatRow(gameModel.getUpperRow()));
        System.out.println("Lower row (cards): " + formatRow(gameModel.getLowerRow()));
        if (!gameModel.getUpperRowBuildings().isEmpty()) {
            System.out.println("Upper row (buildings): "
                    + formatRow(gameModel.getUpperRowBuildings()));
        }
        if (!gameModel.getLowerRowBuildings().isEmpty()) {
            System.out.println("Lower row (buildings): "
                    + formatRow(gameModel.getLowerRowBuildings()));
        }
        System.out.println("Deck remaining: " + gameModel.getDeckRemainingCount());
    }

    private void renderPlayers() {
        System.out.println("--- PLAYERS ---");
        for (String n : gameModel.getTurnOrder()) {
            int food = gameModel.getFoodByPlayer().getOrDefault(n, 0);
            int pp = gameModel.getPpByPlayer().getOrDefault(n, 0);
            int remU = gameModel.getRemainingUpper().getOrDefault(n, 0);
            int remL = gameModel.getRemainingLower().getOrDefault(n, 0);
            String marker = n.equals(gameModel.getMyNickname()) ? " (YOU)" : "";
            System.out.printf("  %-15s  Food: %2d  PP: %3d  Picks: upper=%d lower=%d%s%n",
                    n, food, pp, remU, remL, marker);
            List<String> chars =
                    gameModel.getCharactersByPlayer().getOrDefault(n, List.of());
            List<String> builds =
                    gameModel.getBuildingsByPlayer().getOrDefault(n, List.of());
            if (!chars.isEmpty())  System.out.println("      characters: "
                    + String.join(", ", chars));
            if (!builds.isEmpty()) System.out.println("      buildings:  "
                    + String.join(", ", builds));
        }
    }

    private void renderCommands(PhaseType phase) {
        if (phase == null) return;
        boolean isMyTurn = gameModel.getMyNickname().equals(gameModel.getCurrentPlayer());
        switch (phase) {
            case TOTEM_PLACEMENT -> {
                System.out.println("Commands:");
                if (isMyTurn) {
                    System.out.println("  move <tileID>   — place your totem (e.g. move B)");
                } else {
                    System.out.println("  Waiting for "
                            + gameModel.getCurrentPlayer() + " to place their totem...");
                }
            }
            case ACTION_RESOLUTION -> {
                System.out.println("Commands:");
                if (isMyTurn) {
                    System.out.println("  resolve <id1> [id2 ...]  — pick card IDs from the board");
                    System.out.println("  move T                   — return your totem (end your turn)");
                } else {
                    System.out.println("  Waiting for "
                            + gameModel.getCurrentPlayer() + " to resolve actions...");
                }
            }
            default -> System.out.println("  (waiting for server...)");
        }
    }

    // ==================== UTILS ====================

    private String formatRow(List<String> row) {
        return row.isEmpty() ? "(empty)" : String.join(", ", row);
    }

    private void printHeader() {
        System.out.println("========================================");
        System.out.println("          MESOS - BOARD GAME            ");
        System.out.println("========================================");
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}