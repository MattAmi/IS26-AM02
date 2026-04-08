package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class EndGameFullSetPPEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private EndGameFullSetPPEffect effect;

    // Mock values
    private final int BONUS_PP_PER_SET = 3;

    @BeforeEach
    void setUp() {
        // Mock creation
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Effect instantiation
        effect = new EndGameFullSetPPEffect(BONUS_PP_PER_SET, mockTribu);
    }

    @Test
    void testAcceptCallsVisitPhaseObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    @Test
    void testOnPhaseChange_WithEndGamePhase_PerfectSets() {
        // Setup: The player has exactly 2 of EVERY character type
        int simulatedCharacterCount = 2;
        int expectedPrestigePoints = BONUS_PP_PER_SET * simulatedCharacterCount;

        // any() tells Mockito to return 2 no matter which CharacterType is asked for
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(simulatedCharacterCount);

        // Triggering effect activation
        effect.onPhaseChange(PhaseType.END_GAME);

        // Verify that 2 sets * 3 bonus = 6 points are added
        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestigePoints);
    }

    @Test
    void testOnPhaseChange_WithEndGamePhase_UnevenSets() {
        // Setup: The player has 3 of most characters, but only 1 of SHAMAN.
        // The minimum (the bottleneck) is 1, so they only have 1 full set.
        int baseCharacterCount = 3;
        int bottleneckCount = 1;
        int expectedPrestigePoints = BONUS_PP_PER_SET * bottleneckCount;

        // Set default return to 3
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(baseCharacterCount);
        // Override specifically for SHAMAN to be the bottleneck
        when(mockTribu.getCharacterCount(CharacterType.SHAMAN)).thenReturn(bottleneckCount);

        // Triggering effect activation
        effect.onPhaseChange(PhaseType.END_GAME);

        // Verify that the logic correctly identifies the minimum (1 set)
        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestigePoints);
    }

    @Test
    void testOnPhaseChange_WithEndGamePhase_MissingCharacterForSet() {
        // Setup: The player has 5 of almost everything, but 0 of one specific type.
        // This means 0 full sets completed.
        int baseCharacterCount = 5;
        int bottleneckCount = 0;
        int expectedPrestigePoints = 0; // 0 sets * 3 = 0

        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(baseCharacterCount);
        when(mockTribu.getCharacterCount(CharacterType.SHAMAN)).thenReturn(bottleneckCount);

        // Triggering effect activation
        effect.onPhaseChange(PhaseType.END_GAME);

        // Verify that it adds 0 points without throwing errors
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