package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a card selection contains the same card ID more than once.
 * Each physical card on the board can only be taken once per action.
 */
public class DuplicateCardSelectionException extends GameRuleException {

    /**
     * @param cardID the card ID that appears more than once in the selection
     */
    public DuplicateCardSelectionException(String cardID) {
        super("Duplicate card in selection: " + cardID);
    }
}