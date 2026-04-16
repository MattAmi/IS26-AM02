package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class EndGameCharacterPrestigeEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private EndGameCharacterPrestigeEffect effect;

    // Mock value
    private final int BONUS_PER_CHARACTER = 2;
    private final CharacterType TEST_CHARACTER = CharacterType.SHAMAN;

    @BeforeEach
    void setUp() {
        // Mock creation
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Effect instantiation
        effect = new EndGameCharacterPrestigeEffect(mockTribu, TEST_CHARACTER, BONUS_PER_CHARACTER);
    }

    @Test
    void testAcceptCallsVisitPhaseObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    @Test
    void testOnPhaseChange_WithEndGamePhase_ShouldAddPoints() {
        // Explicit math setup
        int simulatedCharacterCount = 3;
        int expectedPrestigePoints = BONUS_PER_CHARACTER * simulatedCharacterCount;

        // GetCharacterCount of mockTribu will return 3
        when(mockTribu.getCharacterCount(TEST_CHARACTER)).thenReturn(simulatedCharacterCount);

        // Triggering effect activation
        effect.onPhaseChange(PhaseType.END_GAME);

        // Verify that exactly 6 points are added
        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestigePoints);
    }

    @Test
    void testOnPhaseChange_WithZeroCharacters_ShouldAddZeroPoints() {

        int simulatedCharacterCount = 0;
        int expectedPrestigePoints = 0; // 0 * 2 = 0

        when(mockTribu.getCharacterCount(TEST_CHARACTER)).thenReturn(simulatedCharacterCount);

        effect.onPhaseChange(PhaseType.END_GAME);

        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestigePoints);
    }

    @Test
    void testOnPhaseChange_WithOtherPhases_ShouldDoNothing() {
        // Trigger effect when it shouldn't activate
        effect.onPhaseChange(PhaseType.ACTION_RESOLUTION);

        // Verify that it is not actually triggered
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        verify(mockTribu, never()).getCharacterCount(any());
    }
}