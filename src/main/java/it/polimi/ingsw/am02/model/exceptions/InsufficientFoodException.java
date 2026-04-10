package it.polimi.ingsw.am02.model.exceptions;

/**
 * Thrown when a player attempts to purchase a building but does not
 * have enough food points to cover the cost (after builder discounts).
 */
public class InsufficientFoodException extends RuntimeException {

    public InsufficientFoodException(String cardID, int cost, int available) {
        super("Insufficient food to purchase building '" + cardID
                + "': costs " + cost + ", available " + available + ".");
    }
}
