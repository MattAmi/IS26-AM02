package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a tile ID does not correspond to any tile on the board.
 * Covers offer-tile lookups and any other tile-based resolution.
 */
public class TileNotFoundException extends GameRuleException {

    /**
     * @param tileID the single-character identifier of the missing tile
     */
    public TileNotFoundException(char tileID) {
        super("No tile found with ID: '" + tileID + "'.");
    }
}
