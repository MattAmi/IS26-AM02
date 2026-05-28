package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a player attempts to return their totem to the
 * {@link it.polimi.ingsw.am02.server.model.TurnOrderTile} before satisfying
 * all mandatory pick obligations (i.e. remaining upper or lower picks are
 * still non-zero and eligible cards are available).
 */
public class PickObligationNotFulfilledException extends GameRuleException {

    /** Constructs the exception with a fixed message. */
    public PickObligationNotFulfilledException() {
        super("Player has not fulfilled their pick obligations.");
    }
}
