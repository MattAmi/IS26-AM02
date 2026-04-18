package it.polimi.ingsw.am02.server.model.exceptions;

public abstract class GameRuleException extends RuntimeException {
    public GameRuleException(String message) {
        super(message);
    }
}
