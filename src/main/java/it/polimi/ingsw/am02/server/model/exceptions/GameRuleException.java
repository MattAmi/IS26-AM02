package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Abstract base class for all domain-level rule violations.
 *
 * <p>Subclasses represent distinct illegal game actions (wrong turn,
 * card not found, insufficient resources, etc.). The server controller
 * catches these and forwards the message to the offending client via
 * {@link it.polimi.ingsw.am02.common.interfaces.VirtualView#notifyError}.
 */
public abstract class GameRuleException extends RuntimeException {

    /**
     * @param message a human-readable description of the violated rule
     */
    public GameRuleException(String message) {
        super(message);
    }
}
