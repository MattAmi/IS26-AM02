package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when the system expects a player to be occupying an
 * {@link it.polimi.ingsw.am02.server.model.OfferTile} but no tile holds
 * that player. Indicates a state inconsistency in the offer track.
 */
public class PlayerNotOnTileException extends GameRuleException {

    /**
     * @param nickname the nickname of the player not found on any tile
     */
    public PlayerNotOnTileException(String nickname) {
        super("Player '" + nickname + "' is not on any offer tile.");
    }
}
