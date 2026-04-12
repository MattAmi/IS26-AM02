package it.polimi.ingsw.am02.model.exceptions;

/**
 * Thrown when a player attempts to return their totem to the
 * {@code TurnOrderTile} without having fulfilled their mandatory
 * pick obligations (remaining upper/lower picks are not zero).
 */
public class PickObligationNotFulfilledException extends RuntimeException {

    public PickObligationNotFulfilledException() {
        super("Player has not fulfilled their pick obligations.");
    }
}
