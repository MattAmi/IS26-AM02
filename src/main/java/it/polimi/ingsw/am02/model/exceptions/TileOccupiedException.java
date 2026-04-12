package it.polimi.ingsw.am02.model.exceptions;

/**
 * Thrown when a player tries to place their totem on an {@code OfferTile}
 * that is already occupied by another player's totem.
 */
public class TileOccupiedException extends RuntimeException {

    public TileOccupiedException(char tileID) {
        super("Offer tile '" + tileID + "' is already occupied.");
    }
}
