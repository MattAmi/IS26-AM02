package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.listeners.GameEventEmitter;
import it.polimi.ingsw.am02.server.model.tile.TurnOrderTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


class GameBoardSetupTest {

    private GameBoard gameBoard;
    private List<Player> players;
    private final GameEventEmitter notifier = Mockito.mock(GameEventEmitter.class);
    private final Random gameRandom = new Random(42);

    @BeforeEach
    void setUp() {
        TestHelper.ensureRegistryLoaded();

        int numPlayers = 5;
        gameBoard = new GameBoard(numPlayers, notifier, gameRandom);

        players = new ArrayList<>();
        Player p1 = new Player("P1", Totem.RED);
        Player p2 = new Player("P2", Totem.BLUE);
        Player p3 = new Player("P3", Totem.PURPLE);
        Player p4 = new Player("P4", Totem.YELLOW);
        Player p5 = new Player("P5", Totem.WHITE);

        Collections.addAll(players, p1, p2, p3, p4, p5);
    }

    @Test
    void setUpInitialTurnOrderRegistersPlayersInCorrectOrder() {
        gameBoard.setUpInitialTurnOrder(players);

        // Retrieve players from TurnOrderTile — should be in the same order
        TurnOrderTile turnOrderTile = gameBoard.getTurnOrderTile();
        List<Player> orderedPlayers = turnOrderTile.getOrderedPlayers();

        assertEquals(players.size(), orderedPlayers.size(),
                "All players should be registered on the TurnOrderTile");

        for (int i = 0; i < players.size(); i++) {
            assertSame(players.get(i), orderedPlayers.get(i),
                    "Player at position " + i + " should match the input order");
        }
    }

    @Test
    void setUpInitialTurnOrderAssignsCorrectInitialFood() {
        // Verify all players start with 0 food before setup
        for (Player player : players) {
            assertEquals(0, player.getTribu().getFoodPoints(),
                    "Players should start with 0 food before setup");
        }

        gameBoard.setUpInitialTurnOrder(players);

        int[] expectedFood = {2, 3, 3, 4, 4};
        for (int i = 0; i < players.size(); i++) {
            assertEquals(expectedFood[i], players.get(i).getTribu().getFoodPoints(),
                    "Player at position " + i + " should receive " + expectedFood[i] + " food");
        }
    }

    @Test
    void setUpInitialTurnOrderWithFewerPlayers() {
        // Test with 3 players — only first 3 food bonuses apply (2, 3, 3)
        int numPlayers = 3;
        gameBoard = new GameBoard(numPlayers, notifier, gameRandom);

        Player player1 = new Player("Matteo", Totem.WHITE);
        Player player2 = new Player("Raed", Totem.BLUE);
        Player player3 = new Player("Husnain", Totem.RED);

        List<Player> threePlayers = new ArrayList<>(List.of(player1, player2, player3));

        gameBoard.setUpInitialTurnOrder(threePlayers);

        int[] expectedFood = {2, 3, 3};
        for (int i = 0; i < threePlayers.size(); i++) {
            assertEquals(expectedFood[i], threePlayers.get(i).getTribu().getFoodPoints(),
                    "Player at position " + i + " should receive " + expectedFood[i] + " food");
        }

        // Verify registration order
        TurnOrderTile tile = gameBoard.getTurnOrderTile();
        List<Player> ordered = tile.getOrderedPlayers();
        assertEquals(numPlayers, ordered.size());
        for (int i = 0; i < numPlayers; i++) {
            assertSame(threePlayers.get(i), ordered.get(i));
        }
    }

    @Test
    void setUpInitialTurnOrderWithTwoPlayers() {
        // Test with 2 players — minimum count, food: (2, 3)
        int numPlayers = 2;
        gameBoard = new GameBoard(numPlayers, notifier, gameRandom);

        Player player1 = new Player("Matteo", Totem.WHITE);
        Player player2 = new Player("Raed", Totem.BLUE);

        List<Player> twoPlayers = new ArrayList<>(List.of(player1, player2));

        gameBoard.setUpInitialTurnOrder(twoPlayers);

        int[] expectedFood = {2, 3};
        for (int i = 0; i < twoPlayers.size(); i++) {
            assertEquals(expectedFood[i], twoPlayers.get(i).getTribu().getFoodPoints(),
                    "Player at position " + i + " should receive " + expectedFood[i] + " food");
        }
    }
}