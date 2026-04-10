package it.polimi.ingsw.am02.model.exceptions;

/**
 * Thrown when the system expects a player to be occupying an {@code OfferTile}
 * but no tile holds that player. This typically signals a state inconsistency.
 */
public class PlayerNotOnTileException extends RuntimeException {

    public PlayerNotOnTileException(String nickname) {
        super("Player '" + nickname + "' is not on any offer tile.");
    }
}
