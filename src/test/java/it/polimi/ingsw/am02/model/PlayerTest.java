package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player francesco;

     @BeforeEach
     void setUp() {
         francesco = new Player("Francesco", Totem.YELLOW);
     }

    @Test
    @DisplayName("getNickname should return the name set at construction")
    void shouldReturnNickname() {
        assertEquals("Francesco", francesco.getNickname());
    }

    @Test
    @DisplayName("player should have a non-null totem after construction (and in this case it should be YELLOW")
    void shouldHaveTotem() {
        assertNotNull(francesco.getTotem());
        assertEquals(Totem.YELLOW, francesco.getTotem());
    }

}