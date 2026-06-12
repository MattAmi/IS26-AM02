package it.polimi.ingsw.am02.server.model.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the model's custom exceptions, verifying that each produces the
 * expected human-readable message.
 */
class ExceptionsTest {
    /** Verifies the message of {@link InvalidMoveException} is preserved as given. */
    @Test
    void testInvalidMoveException() {
        InvalidMoveException ex = new InvalidMoveException("Invalid move");
        assertEquals("Invalid move", ex.getMessage());
    }

    /** Verifies {@link NotYourTurnException} builds the message from the player name. */
    @Test
    void testNotYourTurnException() {
        NotYourTurnException ex = new NotYourTurnException("Alice");
        assertEquals("It is not Alice's turn.", ex.getMessage());
    }

    /** Verifies {@link PickObligationNotFulfilledException} uses its default message. */
    @Test
    void testPickObligationNotFulfilledException() {
        PickObligationNotFulfilledException ex = new PickObligationNotFulfilledException();
        assertEquals("Player has not fulfilled their pick obligations.", ex.getMessage());
    }

    /** Verifies {@link PlayerNotFoundException} builds the message from the nickname. */
    @Test
    void testPlayerNotFoundException() {
        PlayerNotFoundException ex = new PlayerNotFoundException("Alice");
        assertEquals("No player found with nickname: Alice", ex.getMessage());
    }
}
