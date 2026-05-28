package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a player attempts to purchase one or more buildings but
 * does not have enough food points to cover the total cost after
 * applying building discounts.
 */
public class InsufficientFoodException extends GameRuleException {

    /**
     * @param totalCost the total food required for the selection
     * @param available the player's current food points
     */
    public InsufficientFoodException(int totalCost, int available) {
        super("Insufficient food: cost=" + totalCost + ", available=" + available +
                " (missing " + (totalCost - available) + ")");
    }
}
