package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a player attempts to return their totem to the
 * {@code TurnOrderTile} without having fulfilled their mandatory
 * pick obligations (remaining upper/lower picks are not zero).
 */
public class PickObligationNotFulfilledException extends GameRuleException {

    public PickObligationNotFulfilledException() {
        super("Player has not fulfilled their pick obligations.");
    }
}
