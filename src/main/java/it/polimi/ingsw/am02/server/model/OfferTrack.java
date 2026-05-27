package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.server.model.exceptions.PlayerNotOnTileException;
import it.polimi.ingsw.am02.server.model.exceptions.TileNotFoundException;
import it.polimi.ingsw.am02.server.model.exceptions.TileOccupiedException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The offer track: the ordered collection of {@link OfferTile}s on which players
 * place their totems during the totem-placement phase.
 *
 * <p>Tiles are sorted by tile ID and filtered by player count on construction.
 */
public class OfferTrack {

    private final List<OfferTile> tiles;

    /**
     * Builds the offer track for the given player count, pulling tile templates from
     * {@link GameRegistry} and sorting them by tile ID.
     *
     * @param numPlayers the number of players in the game
     */
    public OfferTrack(int numPlayers) {
        this.tiles = new ArrayList<>();
        setUpTiles(numPlayers);
    }

    private void setUpTiles(int numPlayers) {
        List<OfferTile> allTiles = GameRegistry.getInstance().getOfferTiles(numPlayers);

        for (OfferTile tile : allTiles)
            tiles.add(new OfferTile(tile.getTileID(), tile.getMinPlayers(), tile.getGainedFood(),
                    tile.getNumUpperChoosable(), tile.getNumLowerChoosable()));

        tiles.sort(Comparator.comparingInt(OfferTile::getTileID));
    }

    /**
     * Places the given player on the tile with the specified ID.
     *
     * @param player the player to place
     * @param tileID the target tile identifier
     * @throws TileOccupiedException if the tile is already occupied
     * @throws TileNotFoundException if no tile with this ID exists
     */
    public void occupyTile(Player player, char tileID) {
        for (OfferTile tile : tiles) {
            if (tile.getTileID() == tileID) {
                if (tile.isOccupied()) {
                    throw new TileOccupiedException(tileID);
                }

                tile.acceptPlayer(player);
                return;
            }
        }
        throw new TileNotFoundException(tileID);
    }

    /**
     * @return all players currently on the track, in ascending tile-ID order
     *         (i.e. action-resolution order)
     */
    public List<Player> getOrderedPlayers() {
        List<Player> orderedPlayers = new ArrayList<>();
        for (OfferTile tile : tiles) {
            if (tile.isOccupied()) {
                orderedPlayers.add(tile.getOccupyingPlayer());
            }
        }
        return orderedPlayers;
    }

    /**
     * Finds and returns the tile currently occupied by the given player.
     *
     * @param player the player to look up
     * @return the {@link OfferTile} the player is standing on
     * @throws PlayerNotOnTileException if the player is not on any tile
     */
    public OfferTile getTileByPlayer(Player player) {
        for (OfferTile tile : tiles) {
            if (tile.isOccupied() && tile.getOccupyingPlayer().equals(player)) {
                return tile;
            }
        }
        throw new PlayerNotOnTileException(player.getNickname());
    }

    /**
     * @return a list of {@link OfferTileInfo} DTOs for all tiles on the track,
     *         suitable for sending to clients
     */
    public List<OfferTileInfo> getTilesInfo() {
        return tiles.stream()
                .map(OfferTile::toInfo)
                .toList();
    }
}