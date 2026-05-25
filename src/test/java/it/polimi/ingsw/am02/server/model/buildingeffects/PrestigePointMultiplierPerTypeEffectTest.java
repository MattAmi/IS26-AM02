package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link PrestigePointMultiplierPerTypeEffect} class.
 * Ensures the effect applies a prestige point multiplier specifically to builders
 * at the END_GAME phase, correctly subtracting the base points already awarded (multiplier - 1).
 */
class PrestigePointMultiplierPerTypeEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private PrestigePointMultiplierPerTypeEffect effect;

    // Strict test constants
    private static final int MULTIPLIER = 3;
    private static final String PLAYER_NICKNAME = "DeltaPlayer";
    // Keeping IGNORED_TYPE only if you refused to fix the constructor.
    // Recommended: Remove CharacterType from constructor.
    private static final CharacterType IGNORED_TYPE = CharacterType.BUILDER;

    @BeforeEach
    void setUp() {
        // Arrange
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        // Instantiate the effect.
        effect = new PrestigePointMultiplierPerTypeEffect(mockPlayer, IGNORED_TYPE, MULTIPLIER);
    }

    @Test
    void accept_validVisitor_callsVisitPhaseObserver() {
        // Act
        effect.accept(mockVisitor);

        // Assert
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    // =========================================================================================
    // ON PHASE CHANGE TESTS
    // =========================================================================================

    /**
     * Tests the core business rule: the bonus added must be builders * (multiplier - 1)
     * to avoid double-counting the base points already inherently awarded by the builders.
     */
    @Test
    void onPhaseChange_endGamePhaseWithBuilders_appliesCorrectMultiplierMathAndReturnsDelta() {
        // Arrange
        int simulatedBuilders = 4;
        int expectedBonusPoints = simulatedBuilders * (MULTIPLIER - 1); // 4 * (3-1) = 8

        when(mockTribu.getPPBuilders()).thenReturn(simulatedBuilders);
        when(mockTribu.getPrestigePoints()).thenReturn(20);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: Math verification
        verify(mockTribu, times(1)).addPrestigePoints(expectedBonusPoints);

        // Assert: DTO verification
        assertNotNull(outcome);
        assertFalse(outcome.isEmpty());
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(ResourceType.PRESTIGE_POINTS, outcome.resourceDeltas().get(0).resource());
        assertEquals(expectedBonusPoints, outcome.resourceDeltas().get(0).delta());
    }

    /**
     * Tests that if the multiplier is strictly 1 (meaning "1x points"), the effect
     * gracefully does nothing, as the base points are already accounted for.
     */
    @Test
    void onPhaseChange_multiplierOfOne_addsZeroPointsAndReturnsEmpty() {
        // Arrange: Recreate effect with multiplier 1
        PrestigePointMultiplierPerTypeEffect weakEffect =
                new PrestigePointMultiplierPerTypeEffect(mockPlayer, IGNORED_TYPE, 1);

        int simulatedBuilders = 5;
        when(mockTribu.getPPBuilders()).thenReturn(simulatedBuilders);

        // Act
        EffectOutcome outcome = weakEffect.onPhaseChange(PhaseType.END_GAME);

        // Assert: 5 * (1 - 1) = 0. Verify no points added and empty outcome.
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }

    /**
     * Tests early exit when the player has no builders.
     */
    @Test
    void onPhaseChange_zeroBuilders_addsZeroPointsAndReturnsEmpty() {
        // Arrange
        when(mockTribu.getPPBuilders()).thenReturn(0);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: 0 * (3 - 1) = 0
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }

    /**
     * Tests that the effect remains completely dormant during any phase other than END_GAME.
     */
    @ParameterizedTest
    @EnumSource(value = PhaseType.class, mode = EnumSource.Mode.EXCLUDE, names = {"END_GAME"})
    @NullSource
    void onPhaseChange_nonEndGameOrNullPhase_doesNothingAndReturnsEmpty(PhaseType phase) {
        // Act
        EffectOutcome outcome = effect.onPhaseChange(phase);

        // Assert
        verify(mockPlayer, never()).getTribu();
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }
}