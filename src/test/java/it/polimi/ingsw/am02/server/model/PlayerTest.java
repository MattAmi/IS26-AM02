package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.player.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link Player}, verifying its identity (nickname, totem, tribù).
 */
class PlayerTest {
    /** Verifies a player's accessors. */
    @Test
    void testPlayer() {
        Player player = new Player("Francesco", Totem.RED);
        assertEquals("Francesco", player.getNickname());
        assertEquals(Totem.RED, player.getTotem());
        assertNotNull(player.getTribu());
    }
}
