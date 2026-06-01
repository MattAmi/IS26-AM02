package it.polimi.ingsw.am02.server.model.exceptions;

import it.polimi.ingsw.am02.server.model.tile.OfferTile;

/**
 * Thrown when a player tries to place their totem on an
 * {@link OfferTile} that is already
 * occupied by another player's totem.
 */
public class TileOccupiedException extends GameRuleException {

    /**
     * @param tileID the single-character identifier of the occupied tile
     */
    public TileOccupiedException(char tileID) {
        super("Offer tile '" + tileID + "' is already occupied.");
    }
}
