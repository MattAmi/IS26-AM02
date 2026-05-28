package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a player tries to place their totem on an
 * {@link it.polimi.ingsw.am02.server.model.OfferTile} that is already
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
