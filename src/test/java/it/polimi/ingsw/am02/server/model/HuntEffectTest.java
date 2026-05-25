package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HuntEffectTest {

    private Player p1, p2;
    private Tribu t1, t2;
    private HuntEffect effect;
    private static final int FOOD_PER_HUNTER = 2;
    private static final int PP_PER_HUNTER = 3;

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

        effect = new HuntEffect(FOOD_PER_HUNTER, PP_PER_HUNTER);
    }

    @Test
    void applyEffect_withHunters_addsFoodAndPP() {
        when(t1.getCharacterCount(CharacterType.HUNTER)).thenReturn(2);
        when(t1.getFoodPoints()).thenReturn(10);
        when(t1.getPrestigePoints()).thenReturn(5);
        when(t2.getCharacterCount(CharacterType.HUNTER)).thenReturn(0);

        EffectOutcome outcome = effect.applyEffect(List.of(p1, p2));

        verify(t1).addFoodPoints(4);
        verify(t1).addPrestigePoints(6);
        verify(t2, never()).addFoodPoints(anyInt());
        
        assertEquals(2, outcome.resourceDeltas().size());
    }

    @Test
    void applyEffect_noHunters_doesNothing() {
        when(t1.getCharacterCount(CharacterType.HUNTER)).thenReturn(0);

        EffectOutcome outcome = effect.applyEffect(List.of(p1));

        verify(t1, never()).addFoodPoints(anyInt());
        verify(t1, never()).addPrestigePoints(anyInt());
        assertTrue(outcome.isEmpty());
    }
}
