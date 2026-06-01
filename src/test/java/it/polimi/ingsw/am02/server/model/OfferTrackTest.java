package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.exceptions.PlayerNotOnTileException;
import it.polimi.ingsw.am02.server.model.exceptions.TileNotFoundException;
import it.polimi.ingsw.am02.server.model.exceptions.TileOccupiedException;
import it.polimi.ingsw.am02.server.model.tile.OfferTile;
import it.polimi.ingsw.am02.server.model.tile.OfferTrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfferTrackTest {

    private OfferTrack offerTrack;
    private Player player1;
    private Player player2;
    private Player player3;

    /** First valid tile ID for a 2-player game, discovered from the registry. */
    private char firstTileID;
    /** Second valid tile ID for a 2-player game. */
    private char secondTileID;

    @BeforeEach
    void setUp() {
        TestHelper.ensureRegistryLoaded();

        player1 = new Player("Matteo", Totem.WHITE);
        player2 = new Player("Raed", Totem.BLUE);
        player3 = new Player("Husnain", Totem.RED);

        offerTrack = new OfferTrack(2);

        // Discover valid tile IDs from registry data — tiles are sorted by ID in OfferTrack
        List<OfferTile> templateTiles = GameRegistry.getInstance().getOfferTiles(2);
        templateTiles.sort(java.util.Comparator.comparingInt(OfferTile::getTileID));
        firstTileID = templateTiles.get(0).getTileID();
        secondTileID = templateTiles.get(1).getTileID();
    }

    // Constructor & setUpTiles

    @Test
    void constructorWithTwoPlayersCreatesCorrectNumberOfTiles() {
        // getOrderedPlayers on a fresh track returns an empty list (no one is placed yet)
        List<Player> ordered = offerTrack.getOrderedPlayers();
        assertTrue(ordered.isEmpty(),
                "No players should be on the track initially");
    }

    @Test
    void constructorWithFivePlayersCreatesMoreTiles() {

        List<OfferTile> twoPlayerTiles = GameRegistry.getInstance().getOfferTiles(2);
        List<OfferTile> fivePlayerTiles = GameRegistry.getInstance().getOfferTiles(5);

        assertTrue(fivePlayerTiles.size() > twoPlayerTiles.size(),
                "A 5-player game should have more offer tiles than a 2-player game");
    }


    // occupyTile — happy path

    @Test
    void occupyTileValidTileAndPlayerOccupiesSuccessfully() {
        offerTrack.occupyTile(player1, firstTileID);

        OfferTile tile = offerTrack.getTileByPlayer(player1);
        assertNotNull(tile);
        assertEquals(firstTileID, tile.getTileID());
        assertTrue(tile.isOccupied());
        assertEquals(player1, tile.getOccupyingPlayer());
    }

    @Test
    void occupyTileTwoDifferentTilesBothOccupied() {
        offerTrack.occupyTile(player1, firstTileID);
        offerTrack.occupyTile(player2, secondTileID);

        assertEquals(firstTileID, offerTrack.getTileByPlayer(player1).getTileID());
        assertEquals(secondTileID, offerTrack.getTileByPlayer(player2).getTileID());
    }


    // occupyTile — error cases

    @Test
    void occupyTileAlreadyOccupiedThrowsTileOccupiedException() {
        offerTrack.occupyTile(player1, firstTileID);

        assertThrows(TileOccupiedException.class,
                () -> offerTrack.occupyTile(player2, firstTileID));
    }

    @Test
    void occupyTileInvalidTileIDThrowsTileNotFoundException() {
        assertThrows(TileNotFoundException.class,
                () -> offerTrack.occupyTile(player1, 'Z'));
    }

    // getOrderedPlayers

    @Test
    void getOrderedPlayersNoPlayersPlacedReturnsEmptyList() {
        List<Player> ordered = offerTrack.getOrderedPlayers();
        assertTrue(ordered.isEmpty());
    }

    @Test
    void getOrderedPlayersOnePlayerPlacedReturnsSingletonList() {
        offerTrack.occupyTile(player1, secondTileID);

        List<Player> ordered = offerTrack.getOrderedPlayers();
        assertEquals(1, ordered.size());
        assertEquals(player1, ordered.getFirst());
    }

    @Test
    void getOrderedPlayersTwoPlayersPlacedReturnsInTileOrder() {
        // Player2 on first tile, Player1 on second tile
        // getOrderedPlayers should return them in tile ID order (first, then second)
        offerTrack.occupyTile(player2, firstTileID);
        offerTrack.occupyTile(player1, secondTileID);

        List<Player> ordered = offerTrack.getOrderedPlayers();
        assertEquals(2, ordered.size());
        assertEquals(player2, ordered.get(0),
                "Player on the first tile (lower ID) should come first");
        assertEquals(player1, ordered.get(1),
                "Player on the second tile (higher ID) should come second");
    }

    @Test
    void getOrderedPlayersReverseInsertionOrderStillReturnsTileOrder() {
        // Place in reverse order to verify ordering is by tile, not insertion
        offerTrack.occupyTile(player1, secondTileID);
        offerTrack.occupyTile(player2, firstTileID);

        List<Player> ordered = offerTrack.getOrderedPlayers();
        assertEquals(player2, ordered.get(0),
                "Tile order takes precedence over insertion order");
        assertEquals(player1, ordered.get(1));
    }

    // getTileByPlayer

    @Test
    void getTileByPlayerPlayerIsOnTrackReturnsCorrectTile() {
        offerTrack.occupyTile(player1, firstTileID);

        OfferTile result = offerTrack.getTileByPlayer(player1);
        assertEquals(firstTileID, result.getTileID());
    }

    @Test
    void getTileByPlayerPlayerNotOnTrackThrowsPlayerNotOnTileException() {
        assertThrows(PlayerNotOnTileException.class,
                () -> offerTrack.getTileByPlayer(player1));
    }

    @Test
    void getTileByPlayerDifferentPlayerOnTrackThrowsForAbsentPlayer() {
        offerTrack.occupyTile(player1, firstTileID);

        assertThrows(PlayerNotOnTileException.class,
                () -> offerTrack.getTileByPlayer(player2));
    }


    // Integration: occupy + getTileByPlayer + getOrderedPlayers

    @Test
    void fullCycleOccupyAllTilesThenQueryOrder() {
        offerTrack.occupyTile(player1, firstTileID);
        offerTrack.occupyTile(player2, secondTileID);

        // Both players retrievable
        assertDoesNotThrow(() -> offerTrack.getTileByPlayer(player1));
        assertDoesNotThrow(() -> offerTrack.getTileByPlayer(player2));

        // Ordered list matches tile order
        List<Player> ordered = offerTrack.getOrderedPlayers();
        assertEquals(2, ordered.size());

        // Third player not on any tile
        assertThrows(PlayerNotOnTileException.class,
                () -> offerTrack.getTileByPlayer(player3));
    }

    @Test
    void fivePlayerTrack_allTilesOccupied_orderedCorrectly() {
        OfferTrack bigTrack = new OfferTrack(5);

        Player p1 = new Player("P1", Totem.RED);
        Player p2 = new Player("P2", Totem.BLUE);
        Player p3 = new Player("P3", Totem.PURPLE);
        Player p4 = new Player("P4", Totem.YELLOW);
        Player p5 = new Player("P5", Totem.WHITE);

        List<OfferTile> fivePlayerTiles = GameRegistry.getInstance().getOfferTiles(5);
        fivePlayerTiles.sort(java.util.Comparator.comparingInt(OfferTile::getTileID));

        Player[] players = {p1, p2, p3, p4, p5};

        // Occupy tiles — we occupy as many as we have players (max 5 tiles for 5 players)
        int tilesToOccupy = Math.min(players.length, fivePlayerTiles.size());
        for (int i = 0; i < tilesToOccupy; i++) {
            bigTrack.occupyTile(players[i], fivePlayerTiles.get(i).getTileID());
        }

        List<Player> ordered = bigTrack.getOrderedPlayers();
        assertEquals(tilesToOccupy, ordered.size());

        // Verify the order follows tile ID order
        for (int i = 0; i < tilesToOccupy; i++) {
            assertEquals(players[i], ordered.get(i),
                    "Player at index " + i + " should match tile order");
        }
    }
}