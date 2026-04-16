package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a tile ID does not correspond to any tile on the board.
 * This covers both offer-tile lookups and any other tile-based resolution.
 */
public class TileNotFoundException extends RuntimeException {

    public TileNotFoundException(char tileID) {
        super("No tile found with ID: '" + tileID + "'.");
    }
}
