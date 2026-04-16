package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a player attempts to act outside their turn.
 * The controller should catch this and notify the offending client.
 */
public class NotYourTurnException extends RuntimeException {

    public NotYourTurnException(String nickname) {
        super("It is not " + nickname + "'s turn.");
    }
}