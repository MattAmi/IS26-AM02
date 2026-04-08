package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class PrestigePointMultiplierPerTypeEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private PrestigePointMultiplierPerTypeEffect effect;

    // Strict test constants
    private final int MULTIPLIER = 3;
    // We pass a type because the constructor currently demands it, even though the method ignores it.
    private final CharacterType IGNORED_TYPE = CharacterType.BUILDER;

    @BeforeEach
    void setUp() {
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        effect = new PrestigePointMultiplierPerTypeEffect(MULTIPLIER, IGNORED_TYPE, mockTribu);
    }

    @Test
    void testAcceptCallsVisitPhaseObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    @Test
    void testOnPhaseChange_WithEndGamePhase_ShouldApplyCorrectMultiplierMath() {
        // Arrange: The player has 4 builders.
        int simulatedBuilders = 4;

        // The business rule is: builders * (multiplier - 1)
        // With a multiplier of 3, the bonus is 4 * (3 - 1) = 4 * 2 = 8.
        int expectedBonusPoints = simulatedBuilders * (MULTIPLIER - 1);

        when(mockTribu.getPPBuilders()).thenReturn(simulatedBuilders);

        // Act
        effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: Explicitly verify the exact math outcome
        verify(mockTribu, times(1)).addPrestigePoints(expectedBonusPoints);
    }

    @Test
    void testOnPhaseChange_WithMultiplierOfOne_ShouldAddZeroPoints() {
        // Edge Case: If the multiplier is 1, it means "1x points".
        // Since the builders already give their base points, the extra bonus should be exactly 0.
        int multiplierOfOne = 1;
        PrestigePointMultiplierPerTypeEffect weakEffect =
                new PrestigePointMultiplierPerTypeEffect(multiplierOfOne, IGNORED_TYPE, mockTribu);

        int simulatedBuilders = 5;
        when(mockTribu.getPPBuilders()).thenReturn(simulatedBuilders);

        // Act
        weakEffect.onPhaseChange(PhaseType.END_GAME);

        // Assert: 5 * (1 - 1) = 0. We verify it does not inflate the score.
        verify(mockTribu, times(1)).addPrestigePoints(0);
    }

    @Test
    void testOnPhaseChange_WithZeroBuilders_ShouldAddZeroPoints() {
        // Edge Case: Player has no builders
        int simulatedBuilders = 0;
        when(mockTribu.getPPBuilders()).thenReturn(simulatedBuilders);

        // Act
        effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: 0 * (3 - 1) = 0
        verify(mockTribu, times(1)).addPrestigePoints(0);
    }

    @Test
    void testOnPhaseChange_WithOtherPhases_ShouldDoNothing() {
        // Act: Wrong phase
        effect.onPhaseChange(PhaseType.ACTION_RESOLUTION);

        // Assert: Completely silent
        verify(mockTribu, never()).getPPBuilders();
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }
}