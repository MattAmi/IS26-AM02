package it.polimi.ingsw.am02.server.model.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTest {
    @Test
    void testInvalidMoveException() {
        InvalidMoveException ex = new InvalidMoveException("Invalid move");
        assertEquals("Invalid move", ex.getMessage());
    }

    @Test
    void testNotYourTurnException() {
        NotYourTurnException ex = new NotYourTurnException("Alice");
        assertEquals("It is not Alice's turn.", ex.getMessage());
    }

    @Test
    void testPickObligationNotFulfilledException() {
        PickObligationNotFulfilledException ex = new PickObligationNotFulfilledException();
        assertEquals("Player has not fulfilled their pick obligations.", ex.getMessage());
    }

    @Test
    void testPlayerNotFoundException() {
        PlayerNotFoundException ex = new PlayerNotFoundException("Alice");
        assertEquals("No player found with nickname: Alice", ex.getMessage());
    }
}
