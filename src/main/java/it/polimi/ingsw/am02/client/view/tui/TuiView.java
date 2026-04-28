package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.view.CardCatalog;
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
import java.util.stream.Collectors;

public class TuiView extends AbstractClientView {

    private final LobbyModel lobbyModel;
    private GameModel gameModel;

    public TuiView(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
        lobbyModel.addObserver(this);
    }

    public void onGameModelCreated(GameModel gameModel) {
        this.gameModel = gameModel;
        gameModel.addObserver(this);
    }

    // LOBBY CALLBACKS

    @Override
    public void onUsernameResult(String username, boolean accepted, String reason) {
        if (accepted) {
            clearScreen();
            printHeader();
            System.out.println("Logged in as: " + username);
            System.out.println("\nCommands: create <size> | join <index> | quit");
        } else {
            System.err.println("[ERROR] Username '" + username + "' is not available: " + reason);
        }
        System.out.print("\n> ");
    }

    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        clearScreen();
        printHeader();
        System.out.println("Logged in as: " + lobbyModel.getMyNickname());
        System.out.println("\n--- AVAILABLE LOBBIES ---");
        if (lobbies.isEmpty()) {
            System.out.println("No active lobbies. Create one with: create <size>");
        } else {
            for (int i = 0; i < lobbies.size(); i++) {
                LobbyInfo info = lobbies.get(i);
                System.out.printf("  [%d] Players: %s/%d%n",
                        i, info.currentPlayers(), info.expectedPlayers());
            }
        }
        System.out.println("\nCommands: create <size> | join <index> | reconnect <nickname> <gameID> | quit");
        System.out.print("\n> ");
    }

    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        clearScreen();
        printHeader();
        System.out.println("Logged in as: " + lobbyModel.getMyNickname());
        System.out.println("\n--- LOBBY: " + lobby.lobbyId() + " ---");
        System.out.printf("Players: %d/%d%n",
                lobby.currentPlayers().size(), lobby.expectedPlayers());
        lobby.currentPlayers().forEach(n -> {
            Totem chosen = lobby.chosenTotems().get(n);
            String totemStr = (chosen != null) ? " [" + chosen + "]" : " [no totem]";
            System.out.println("  - " + n + totemStr);
        });
        System.out.println("\nCommands: nick <nickname> | totem <color> | leave | quit");
        System.out.print("\n> ");
    }

    @Override
    public void onLobbyDissolved() {
        System.out.println("\n[LOBBY] The lobby has been dissolved.");
        System.out.print("\n> ");
    }

    // GAME CALLBACKS

    @Override
    public void onGameStarted(String gameId) {
        clearScreen();
        printHeader();
        System.out.println("Game started! ID: " + gameId);
    }

    @Override
    public void onGameSetupCompleted(List<String> turnOrder,
                                     Map<String, Integer> initialFood,
                                     BoardSnapshot board) {
        renderFullGame();
    }

    @Override
    public void onPhaseChanged(PhaseType phase,
                               String currentPlayer,
                               List<String> resolutionOrder) {
        renderFullGame();
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        System.out.println("\n[TURN] Current player: " + nextPlayer);
        System.out.print("\n> ");
    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        System.out.printf("[BOARD] %s placed totem on tile %c%n", nickname, tileID);
        System.out.print("\n> ");
    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        System.out.printf("[BOARD] %s returned totem to slot %d%n",
                nickname, turnOrderPosition);
        System.out.print("\n> ");
    }

    @Override
    public void onBoardUpdated(List<String> newUpperRow,
                               List<String> newLowerRow,
                               int deckRemainingCount) {
        renderFullGame();
    }

    @Override
    public void onEraChanged(List<String> newUpperRowBuildings,
                             List<String> newLowerRowBuildings) {
        System.out.println("\n[ERA] A new era has begun.");
        renderFullGame();
    }

    @Override
    public void onPlayerResourceChanged(String nickname,
                                        ResourceType resource,
                                        int newValue) {
        System.out.printf("[PLAYER] %s — %s: %d%n", nickname, resource, newValue);
        System.out.print("\n> ");
    }

    @Override
    public void onCardTaken(String nickname,
                            String cardID,
                            CardType cardType,
                            RowPosition sourceRow) {
        System.out.printf("[PLAYER] %s took %s from %s row%n",
                nickname, CardCatalog.getInstance().format(cardID), sourceRow);
        System.out.print("\n> ");
    }

    @Override
    public void onEventResolved(String eventName) {
        System.out.println("[EVENT] Resolved: " + eventName);
        System.out.print("\n> ");
    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        System.out.printf("[EXTRA] %s gained an extra turn (upper=%d, lower=%d)%n",
                nickname, remainingUpper, remainingLower);
        System.out.print("\n> ");
    }

    @Override
    public void onExtraTurnEnded(String nickname) {
        System.out.printf("[EXTRA] %s's extra turn ended%n", nickname);
        System.out.print("\n> ");
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
            System.out.printf("  %d. %-15s  PP: %d%n",
                    i + 1, s.nickname(), s.totalPrestigePoints());
        }
        System.out.println("\nType quit to exit.");
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        System.out.println("\n[!] Player disconnected: " + nickname);
        System.out.print("\n> ");
    }

    @Override
    public void onPlayerReconnected(String nickname) {
        System.out.println("\n[!] Player reconnected: " + nickname);
        System.out.print("\n> ");
    }

    @Override
    public void onGameAborted(String lastManStanding) {
        clearScreen();
        printHeader();
        System.out.println("=== GAME ABORTED ===");
        System.out.println("Winner by forfeit: " + lastManStanding);
        System.out.println("\nType quit to exit.");
    }

    @Override
    public void onGameRecoveryFailed() {
        clearScreen();
        printHeader();
        System.out.println("=== GAME RECOVERY FAILED ===");
        System.out.println("Not all players reconnected in time. The game has been terminated.");
        System.out.println("\nType quit to exit.");
    }

    @Override
    public void onError(String message) {
        System.err.println("\n[ERROR] " + message);
        System.out.print("\n> ");
    }

    // FULL RE-RENDER

    private void renderFullGame() {
        if (gameModel == null) return;
        clearScreen();
        printHeader();
        PhaseType phase = gameModel.getCurrentPhase();
        System.out.println("Game: " + gameModel.getGameId());
        System.out.println("Phase: " + (phase != null ? phase : "starting..."));
        System.out.println("Current player: " + gameModel.getCurrentPlayer());
        System.out.println();
        renderBoard();
        System.out.println();
        renderPlayers();
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
        System.out.println("Upper row (cards):");
        gameModel.getUpperRow().forEach(id ->
                System.out.println("  " + CardCatalog.getInstance().format(id)));
        System.out.println("Lower row (cards):");
        gameModel.getLowerRow().forEach(id ->
                System.out.println("  " + CardCatalog.getInstance().format(id)));
        if (!gameModel.getUpperRowBuildings().isEmpty()) {
            System.out.println("Upper row (buildings):");
            gameModel.getUpperRowBuildings().forEach(id ->
                    System.out.println("  " + CardCatalog.getInstance().format(id)));
        }
        if (!gameModel.getLowerRowBuildings().isEmpty()) {
            System.out.println("Lower row (buildings):");
            gameModel.getLowerRowBuildings().forEach(id ->
                    System.out.println("  " + CardCatalog.getInstance().format(id)));
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
            List<String> chars = gameModel.getCharactersByPlayer().getOrDefault(n, List.of());
            List<String> builds = gameModel.getBuildingsByPlayer().getOrDefault(n, List.of());
            if (!chars.isEmpty()) {
                System.out.println("      characters:");
                chars.forEach(id -> System.out.println("        " + CardCatalog.getInstance().format(id)));
            }
            if (!builds.isEmpty()) {
                System.out.println("      buildings:");
                builds.forEach(id -> System.out.println("        " + CardCatalog.getInstance().format(id)));
            }
        }
    }

    private void renderCommands(PhaseType phase) {
        if (phase == null || gameModel == null) return;

        String myNick = gameModel.getMyNickname();
        String currentP = gameModel.getCurrentPlayer();

        // Protezione contro i null durante la fase di inizializzazione asincrona
        if (myNick == null || currentP == null) {
            System.out.println("  (Inizializzazione dati giocatore...)");
            return;
        }

        boolean isMyTurn = myNick.equals(currentP);
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

    // UTILS

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