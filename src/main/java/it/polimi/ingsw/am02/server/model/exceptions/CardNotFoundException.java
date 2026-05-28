package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a card ID cannot be resolved — either on the board rows
 * or inside {@link it.polimi.ingsw.am02.server.model.GameRegistry}.
 * Covers both character/event cards and building cards.
 *
 * @see it.polimi.ingsw.am02.server.model.GameBoard#processActionSelection
 */
public class CardNotFoundException extends GameRuleException {

    /**
     * @param cardID the unresolvable card identifier
     */
    public CardNotFoundException(String cardID) {
        super("Card not found: " + cardID);
    }
}
