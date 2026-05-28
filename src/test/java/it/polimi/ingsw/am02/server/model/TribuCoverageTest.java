package it.polimi.ingsw.am02.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TribuCoverageTest {

    private Tribu tribu;
    private Game game;
    private Player player;

    @BeforeEach
    void setUp() {
        tribu = new Tribu();
        game = Mockito.mock(Game.class);
        player = Mockito.mock(Player.class);
        Mockito.when(player.getNickname()).thenReturn("Bob");
    }

    @Test
    void testImmunityAndLastEventBonus() {
        tribu.setImmuneToShamanicPenalty(true);
        assertTrue(tribu.isImmune());

        tribu.setLastEventBonusReceived(10);
        assertEquals(10, tribu.getLastEventBonusReceived());
    }

    @Test
    void testInsertBuilding() {
        // Ensure GameRegistry is loaded so we can find real cards or use reflection to inject a mock
        // Since B_001 is a real building, let's just test the effect outcome
        TestHelper.ensureRegistryLoaded();
        
        // This should run without throwing CardNotFoundException
        tribu.insertBuilding("B_001", player, game);
        
        assertEquals(3, tribu.getTotalPPBuildings());
        assertEquals(1, tribu.getActiveBuildingEffects().size());
    }
}
