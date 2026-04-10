package it.polimi.ingsw.am02.model.exceptions;

/**
 * Thrown when a card selection exceeds the remaining pick allowance
 * for the upper row, lower row, or extra-turn limits.
 */
public class PickLimitExceededException extends RuntimeException {

    public PickLimitExceededException(String detail) {
        super("Selection exceeds allowed pick limits: " + detail);
    }
}