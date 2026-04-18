package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a nickname cannot be resolved to a known {@code Player} instance.
 * This indicates either a programming error (stale nickname) or an invalid
 * request from the network layer.
 */
public class PlayerNotFoundException extends GameRuleException {

    public PlayerNotFoundException(String nickname) {
        super("No player found with nickname: " + nickname);
    }
}
