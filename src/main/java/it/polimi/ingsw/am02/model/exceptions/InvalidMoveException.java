package it.polimi.ingsw.am02.model.exceptions;

/**
 * Thrown when a move is structurally valid but not permitted in the current
 * game state — e.g. calling {@code moveTotem()} or {@code resolveActions()}
 * during a phase that does not support it, or specifying an illegal
 * destination tile.
 */
public class InvalidMoveException extends RuntimeException {

    public InvalidMoveException(String reason) {
        super(reason);
    }
}
