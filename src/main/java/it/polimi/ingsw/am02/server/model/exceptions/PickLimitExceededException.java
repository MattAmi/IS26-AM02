package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a card selection exceeds the remaining pick allowance
 * for the upper row, lower row, or extra-turn limits on the player's
 * current {@link it.polimi.ingsw.am02.server.model.OfferTile}.
 */
public class PickLimitExceededException extends GameRuleException {

    /**
     * @param detail a description of which limit was exceeded and by how much
     */
    public PickLimitExceededException(String detail) {
        super("Selection exceeds allowed pick limits: " + detail);
    }
}