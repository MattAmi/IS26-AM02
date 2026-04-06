package it.polimi.ingsw.am02.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OfferTrack {

    private List<OfferTile> tiles;

    public OfferTrack(int numPlayers) {
        this.tiles = new ArrayList<>();
        setUpTiles(numPlayers);
    }

    private void setUpTiles(int numPlayers) {
        List<OfferTile> allTiles = GameRegistry.getOfferTiles(numPlayers);
        for (OfferTile tile : allTiles) {
            if (tile.getMinPlayers() <= numPlayers) {
                tiles.add(new OfferTile(tile.getTileID(), tile.getMinPlayers(), tile.getGainedFood(),
                        tile.getNumUpperChoosable(), tile.getNumLowerChoosable()));
            }
        }

        tiles.sort(Comparator.comparingInt(OfferTile::getTileID));
    }

    public void occupyTile(Player player, char tileID) {
        for (OfferTile tile : tiles) {
            if (tile.getTileID() == tileID) {
                if (tile.isOccupied()) {
                    throw new IllegalStateException("Offer tile '" + tileID + "' is already occupied."); // TO DO
                }

                tile.acceptPlayer(player);
                return;
            }
        }
        throw new IllegalArgumentException("No offer tile with ID '" + tileID + "' exists."); // TO DO
    }

    public List<Player> getOrderedPlayers() {
        List<Player> orderedPlayers = new ArrayList<>();
        for (OfferTile tile : tiles) {
            if (tile.isOccupied()) {
                orderedPlayers.add(tile.getOccupyingPlayer());
            }
        }
        return orderedPlayers;
    }

    public OfferTile getTileByPlayer(Player player) {
        for (OfferTile tile : tiles) {
            if (tile.isOccupied() && tile.getOccupyingPlayer().equals(player)) {
                return tile;
            }
        }
        throw new IllegalStateException("Player '" + player.getNickname() + "' is not on any offer tile."); // TO DO

    }
}