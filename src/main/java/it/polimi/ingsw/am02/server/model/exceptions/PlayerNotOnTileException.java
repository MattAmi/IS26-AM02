package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when the system expects a player to be occupying an {@code OfferTile}
 * but no tile holds that player. This typically signals a state inconsistency.
 */
public class PlayerNotOnTileException extends GameRuleException {

    public PlayerNotOnTileException(String nickname) {
        super("Player '" + nickname + "' is not on any offer tile.");
    }
}
