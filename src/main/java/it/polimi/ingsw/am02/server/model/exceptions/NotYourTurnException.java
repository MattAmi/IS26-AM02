package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a player attempts to act outside their designated turn.
 * The controller catches this and notifies only the offending client.
 */
public class NotYourTurnException extends GameRuleException {

    /**
     * @param nickname the nickname of the player who acted out of turn
     */
    public NotYourTurnException(String nickname) {
        super("It is not " + nickname + "'s turn.");
    }
}