package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.model.ClientModel;
import it.polimi.ingsw.am02.client.model.ClientModelObserver;
import it.polimi.ingsw.am02.client.view.View;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;
import java.util.Map;

public class TuiView implements View, ClientModelObserver {

    private final ClientModel model;

    public TuiView(ClientModel model) {
        this.model = model;
        this.model.addObserver(this);
    }

    @Override
    public void update() {
        render();
    }

    public void render() {
        clearScreen();
        printHeader();

        if (model.getMyNickname() == null) {
            renderLogin();
        } else if (model.isGameEnded()) {
            renderFinalScoring();
        } else if (model.isInGame()) {
            renderGame();
        } else if (model.getCurrentLobby() != null) {
            renderLobby();
        } else {
            renderLobbyList();
        }

        System.out.print("\n> ");
    }

    // ------------------------------------------------------------------ lobby

    private void renderLogin() {
        System.out.println("\nWelcome! Please log in.");
        System.out.println("  login <nickname>");
    }

    private void renderLobbyList() {
        System.out.println("Logged in as: " + model.getMyNickname());
        System.out.println("\n--- AVAILABLE LOBBIES ---");
        List<LobbyInfo> lobbies = model.getAvailableLobbies();
        if (lobbies.isEmpty()) {
            System.out.println("No active lobbies. Create one with: create <size>");
        } else {
            for (int i = 0; i < lobbies.size(); i++) {
                LobbyInfo info = lobbies.get(i);
                System.out.printf("  [%d] Players: %s/%d%n",
                        i, info.currentPlayers(), info.expectedPlayers());
            }
        }
        System.out.println("\nCommands: create <size> | join <index> | quit");
    }

    private void renderLobby() {
        LobbyInfo lobby = model.getCurrentLobby();
        System.out.println("Logged in as: " + model.getMyNickname());
        System.out.println("\n--- LOBBY: " + lobby.lobbyId() + " ---");
        System.out.printf("Players: %d/%d%n",
                lobby.currentPlayers().size(), lobby.expectedPlayers());
        lobby.currentPlayers().forEach(n -> {
            Totem chosen = lobby.chosenTotems().get(n);
            String totemStr = (chosen != null) ? " [" + chosen + "]" : " [no totem]";
            System.out.println("  - " + n + totemStr);
        });
        System.out.println("\nCommands: totem <color> | leave | quit");
    }

    // ------------------------------------------------------------------ game

    private void renderGame() {
        PhaseType phase = model.getCurrentPhase();
        System.out.println("Game: " + model.getGameId());
        System.out.println("Phase: " + (phase != null ? phase : "starting..."));
        System.out.println("Current player: " + model.getCurrentPlayer());
        System.out.println();

        renderBoard();
        System.out.println();
        renderPlayers();
        System.out.println();
        renderCommands(phase);

        if (model.getLastEventResolved() != null) {
            System.out.println("\n[Last event resolved]: " + model.getLastEventResolved());
        }
        if (model.getLastErrorMessage() != null) {
            System.out.println("\n[!] " + model.getLastErrorMessage());
        }
    }

    private void renderBoard() {
        System.out.println("--- OFFER TRACK ---");
        for (OfferTileInfo tile : model.getOfferTiles()) {
            String occupant = tile.occupantNickname() != null ? " <- " + tile.occupantNickname() : "";
            System.out.printf("  [%c] food:+%d  upper:%d  lower:%d%s%n",
                    tile.tileID(), tile.foodBonus(),
                    tile.upperChoosable(), tile.lowerChoosable(), occupant);
        }
        System.out.println();
        System.out.println("Upper row (cards): " + formatRow(model.getUpperRow()));
        System.out.println("Lower row (cards): " + formatRow(model.getLowerRow()));
        if (!model.getUpperRowBuildings().isEmpty())
            System.out.println("Upper row (buildings): " + formatRow(model.getUpperRowBuildings()));
        if (!model.getLowerRowBuildings().isEmpty())
            System.out.println("Lower row (buildings): " + formatRow(model.getLowerRowBuildings()));
        System.out.println("Deck remaining: " + model.getDeckRemainingCount());
    }

    private void renderPlayers() {
        System.out.println("--- PLAYERS ---");
        for (String n : model.getTurnOrder()) {
            int food = model.getFoodByPlayer().getOrDefault(n, 0);
            int pp = model.getPpByPlayer().getOrDefault(n, 0);
            int remU = model.getRemainingUpper().getOrDefault(n, 0);
            int remL = model.getRemainingLower().getOrDefault(n, 0);
            String marker = n.equals(model.getMyNickname()) ? " (YOU)" : "";
            System.out.printf("  %-15s  Food: %2d  PP: %3d  Picks: upper=%d lower=%d%s%n",
                    n, food, pp, remU, remL, marker);

            List<String> chars = model.getCharactersByPlayer().getOrDefault(n, List.of());
            List<String> builds = model.getBuildingsByPlayer().getOrDefault(n, List.of());
            if (!chars.isEmpty())  System.out.println("      characters: " + String.join(", ", chars));
            if (!builds.isEmpty()) System.out.println("      buildings:  " + String.join(", ", builds));
        }
    }

    private void renderCommands(PhaseType phase) {
        if (phase == null) return;
        boolean isMyTurn = model.getMyNickname().equals(model.getCurrentPlayer());

        switch (phase) {
            case TOTEM_PLACEMENT -> {
                System.out.println("Commands:");
                if (isMyTurn) System.out.println("  move <tileID>   — place your totem on a tile (e.g. move B)");
                else          System.out.println("  Waiting for " + model.getCurrentPlayer() + " to place their totem...");
            }
            case ACTION_RESOLUTION -> {
                System.out.println("Commands:");
                if (isMyTurn) {
                    System.out.println("  resolve <id1> [id2 ...]  — pick card IDs from the board");
                    System.out.println("  move T                   — return your totem to the TurnOrderTile (end your turn)");
                } else {
                    System.out.println("  Waiting for " + model.getCurrentPlayer() + " to resolve actions...");
                }
            }
            default -> System.out.println("  (waiting for server...)");
        }
    }

    private void renderFinalScoring() {
        System.out.println("=== GAME OVER ===");
        System.out.println("Winners: " + String.join(", ", model.getWinners()));
        System.out.println("\n--- FINAL RANKINGS ---");
        List<PlayerFinalScore> rankings = model.getFinalRankings();
        for (int i = 0; i < rankings.size(); i++) {
            PlayerFinalScore s = rankings.get(i);
            System.out.printf("  %d. %-15s  PP: %d%n", i + 1, s.nickname(), s.totalPrestigePoints());
        }
        System.out.println("\nType quit to exit.");
    }

    // ------------------------------------------------------------------ utils

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

    public void displayError(String message) {
        System.err.println("\n[ERROR] " + message);
    }
}