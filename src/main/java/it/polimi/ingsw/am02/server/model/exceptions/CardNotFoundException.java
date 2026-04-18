package it.polimi.ingsw.am02.server.model.exceptions;

/**
 Thrown when a card ID cannot be found in any row of the board or
 in the GameRegistry. Covers both character/event cards
 and building cards.
 */
public class CardNotFoundException extends GameRuleException {

    public CardNotFoundException(String cardID) {
        super("Card not found: " + cardID);
    }
}
