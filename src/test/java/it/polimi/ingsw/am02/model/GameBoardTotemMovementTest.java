package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the complete totem placement flow on GameBoard:
 * <ul>
 *   <li>{@code movePlayerToOffer(Player, char)} — moving a totem onto an OfferTile</li>
 *   <li>{@code areAllTotemsPlaced()} — checking whether all totems have left the TurnOrderTile</li>
 *   <li>{@code getPlayersInResolutionOrder()} — retrieving players ordered left-to-right on the OfferTrack</li>
 * </ul>
 *
 * Covers: normal placement, duplicate tile rejection, all-placed detection,
 * resolution order matching placement order on the track.
 */
class GameBoardTotemMovementTest {

    private static final int NUM_PLAYERS = 3;

    private GameBoard gameBoard;
    private List<Player> players;
    private GameRegistry registry;

    @BeforeEach
    void setUp() {
        TestHelper.ensureRegistryLoaded();
        registry = GameRegistry.getInstance();

        // Create the board for a 3-player game
        gameBoard = new GameBoard(NUM_PLAYERS);

        players = new ArrayList<>();

        Player player1 = new Player("Matteo", Totem.WHITE);
        Player player2 = new Player("Raed", Totem.BLUE);
        Player player3 = new Player("Francesco", Totem.YELLOW);

        players.add(player1);
        players.add(player2);
        players.add(player3);

        gameBoard.setUpInitialTurnOrder(players);
    }


    //  movePlayerToOffer — happy path

    @Test
    void movePlayerToOfferFirstPlayerPlacedSuccessfully() {
        // The OfferTrack for 3 players should have tiles B, C, D (letters in order).
        // Tile IDs for 3 players: B='B', C='C', D='D'
        assertDoesNotThrow(() -> gameBoard.movePlayerToOffer(players.getFirst(), 'B'));
    }

    @Test
    void movePlayerToOfferAllThreePlayersOnDifferentTiles() {
        gameBoard.movePlayerToOffer(players.get(0), 'B');
        gameBoard.movePlayerToOffer(players.get(1), 'C');
        assertDoesNotThrow(() -> gameBoard.movePlayerToOffer(players.get(2), 'D'));
    }


    //  movePlayerToOffer — error cases

    @Test
    void movePlayerToOffer_tileAlreadyOccupied_throwsException() {
        gameBoard.movePlayerToOffer(players.get(0), 'B');

        // Second player tries the same tile — must be rejected
        assertThrows(RuntimeException.class,
                () -> gameBoard.movePlayerToOffer(players.get(1), 'B'));
    }

    @Test
    void movePlayerToOfferInvalidTileIDThrowsException() {
        // 'Z' does not correspond to any OfferTile for a 3-player game
        assertThrows(RuntimeException.class,
                () -> gameBoard.movePlayerToOffer(players.getFirst(), 'Z'));
    }

    //  areAllTotemsPlaced

    @Test
    void areAllTotemsPlacedNoTotemsPlacedYetReturnsFalse() {
        assertFalse(gameBoard.areAllTotemsPlaced(),
                "No totems moved yet — TurnOrderTile should not be empty");
    }

    @Test
    void areAllTotemsPlacedPartialPlacementReturnsFalse() {
        gameBoard.movePlayerToOffer(players.get(0), 'B');
        gameBoard.movePlayerToOffer(players.get(1), 'C');

        assertFalse(gameBoard.areAllTotemsPlaced(),
                "Only 2 of 3 totems placed — should return false");
    }

    @Test
    void areAllTotemsPlacedAllPlacedReturnsTrue() {
        gameBoard.movePlayerToOffer(players.get(0), 'B');
        gameBoard.movePlayerToOffer(players.get(1), 'C');
        gameBoard.movePlayerToOffer(players.get(2), 'D');

        assertTrue(gameBoard.areAllTotemsPlaced(),
                "All 3 totems placed on OfferTrack — TurnOrderTile should be empty");
    }

    //  getPlayersInResolutionOrder

    @Test
    void getPlayersInResolutionOrderMatchesLeftToRightTrackOrder() {
        // Place totems in non-alphabetical order: player1->D, player2->B, player3->C
        gameBoard.movePlayerToOffer(players.get(0), 'D');
        gameBoard.movePlayerToOffer(players.get(1), 'B');
        gameBoard.movePlayerToOffer(players.get(2), 'C');

        List<Player> resolved = gameBoard.getPlayersInResolutionOrder();

        assertEquals(NUM_PLAYERS, resolved.size(),
                "Resolution order must contain all players");

        // Track order is B < C < D (left to right), so:
        //   position 0 -> player2 (on B)
        //   position 1 -> player3 (on C)
        //   position 2 -> player1 (on D)
        assertEquals(players.get(1), resolved.get(0),
                "Player on tile B should resolve first");
        assertEquals(players.get(2), resolved.get(1),
                "Player on tile C should resolve second");
        assertEquals(players.get(0), resolved.get(2),
                "Player on tile D should resolve third");
    }

    @Test
    void getPlayersInResolutionOrderAlphabeticalPlacementPreservesOrder() {
        // Place in alphabetical tile order: player1->B, player2->C, player3->D
        gameBoard.movePlayerToOffer(players.get(0), 'B');
        gameBoard.movePlayerToOffer(players.get(1), 'C');
        gameBoard.movePlayerToOffer(players.get(2), 'D');

        List<Player> resolved = gameBoard.getPlayersInResolutionOrder();

        assertEquals(players.get(0), resolved.get(0));
        assertEquals(players.get(1), resolved.get(1));
        assertEquals(players.get(2), resolved.get(2));
    }

    //  Full flow: placement → check → resolution order

    @Test
    void fullPlacementFlowPlaceAllThenResolve() {
        // Step 1: verify nobody is placed yet
        assertFalse(gameBoard.areAllTotemsPlaced());

        // Step 2: place totems one by one, checking intermediate state
        gameBoard.movePlayerToOffer(players.get(2), 'B');  // player3 picks first tile
        assertFalse(gameBoard.areAllTotemsPlaced());

        gameBoard.movePlayerToOffer(players.get(0), 'D');  // player1 picks last tile
        assertFalse(gameBoard.areAllTotemsPlaced());

        gameBoard.movePlayerToOffer(players.get(1), 'C');  // player2 fills the middle
        assertTrue(gameBoard.areAllTotemsPlaced(),
                "After all 3 placements, TurnOrderTile must be empty");

        // Step 3: verify resolution order is left-to-right on track (B, C, D)
        List<Player> resolved = gameBoard.getPlayersInResolutionOrder();
        assertEquals(3, resolved.size());
        assertEquals("Francesco", resolved.get(0).getNickname(),
                "Player on B resolves first");
        assertEquals("Raed", resolved.get(1).getNickname(),
                "Player on C resolves second");
        assertEquals("Matteo", resolved.get(2).getNickname(),
                "Player on D resolves third");
    }
}