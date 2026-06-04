package it.polimi.ingsw.am02.server.model.exceptions;

import it.polimi.ingsw.am02.server.model.player.Player;

/**
 * Thrown when a nickname cannot be resolved to a known
 * {@link Player} instance.
 * This typically indicates either a stale nickname reference in the
 * controller or an invalid request from the network layer.
 */
public class PlayerNotFoundException extends GameRuleException {

    /**
     * @param nickname the unresolvable player nickname
     */
    public PlayerNotFoundException(String nickname) {
        super("No player found with nickname: " + nickname);
    }
}
