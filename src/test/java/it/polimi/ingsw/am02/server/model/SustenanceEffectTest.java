package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.effect.SustenanceEffect;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SustenanceEffectTest {

    private Player p1, p2;
    private Tribu t1, t2;
    private SustenanceEffect effect;
    private static final int PENALTY = 2;

    @BeforeEach
    void setUp() {
        p1 = mock(Player.class);
        p2 = mock(Player.class);
        t1 = mock(Tribu.class);
        t2 = mock(Tribu.class);

        when(p1.getTribu()).thenReturn(t1);
        when(p2.getTribu()).thenReturn(t2);
        when(p1.getNickname()).thenReturn("P1");
        when(p2.getNickname()).thenReturn("P2");

        effect = new SustenanceEffect(PENALTY);
    }

    @Test
    void applyEffect_allFed_deductsFood() {
        when(t1.calculateSustenanceCost()).thenReturn(3);
        when(t1.getFoodPoints()).thenReturn(10);
        when(t2.calculateSustenanceCost()).thenReturn(0);
        when(t2.getFoodPoints()).thenReturn(5);

        EffectOutcome outcome = effect.applyEffect(List.of(p1, p2));

        verify(t1).addFoodPoints(-3);
        verify(t1, never()).addPrestigePoints(anyInt());
        verify(t2, never()).addFoodPoints(anyInt());
        
        assertFalse(outcome.isEmpty());
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(ResourceType.FOOD, outcome.resourceDeltas().get(0).resource());
        assertEquals(-3, outcome.resourceDeltas().get(0).delta());
    }

    @Test
    void applyEffect_partialFed_deductsFoodAndPP() {
        when(t1.calculateSustenanceCost()).thenReturn(5);
        when(t1.getFoodPoints()).thenReturn(2); // Shortage of 3

        EffectOutcome outcome = effect.applyEffect(List.of(p1));

        verify(t1).addFoodPoints(-2);
        verify(t1).addPrestigePoints(-(3 * PENALTY));
        
        assertEquals(2, outcome.resourceDeltas().size());
    }

    @Test
    void applyEffect_noneFed_deductsOnlyPP() {
        when(t1.calculateSustenanceCost()).thenReturn(4);
        when(t1.getFoodPoints()).thenReturn(0);

        EffectOutcome outcome = effect.applyEffect(List.of(p1));

        verify(t1, never()).addFoodPoints(anyInt());
        verify(t1).addPrestigePoints(-(4 * PENALTY));
        
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(ResourceType.PRESTIGE_POINTS, outcome.resourceDeltas().get(0).resource());
    }
}
