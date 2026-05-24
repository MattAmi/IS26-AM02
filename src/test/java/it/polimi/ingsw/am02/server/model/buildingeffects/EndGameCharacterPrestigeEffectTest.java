package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
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
 * Unit tests for the {@link EndGameCharacterPrestigeEffect} class.
 * Ensures that the effect correctly calculates and awards prestige points
 * based on the amount of specific characters owned, exclusively during the END_GAME phase.
 */
class EndGameCharacterPrestigeEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private EndGameCharacterPrestigeEffect effect;

    // Constants for test predictability
    private static final int BONUS_PER_CHARACTER = 2;
    private static final CharacterType TEST_CHARACTER = CharacterType.SHAMAN;
    private static final String PLAYER_NICKNAME = "AlphaPlayer";

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Setup common mock behaviors needed by the effect
        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        // Instantiate the effect with predictable parameters
        effect = new EndGameCharacterPrestigeEffect(mockPlayer, TEST_CHARACTER, BONUS_PER_CHARACTER);
    }

    /**
     * Tests that the visitor pattern is correctly implemented for PhaseObservers.
     */
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
     * Tests that the effect calculates the correct bonus and creates a non-empty outcome
     * when the phase is END_GAME and the player has the required characters.
     */
    @Test
    void onPhaseChange_endGamePhaseWithPositiveBonus_addsPointsAndReturnsDelta() {
        // Arrange
        int simulatedCharacterCount = 3;
        int expectedPrestigePoints = BONUS_PER_CHARACTER * simulatedCharacterCount; // 2 * 3 = 6
        int currentTotalPrestige = 15; // Simulated total after addition

        when(mockTribu.getCharacterCount(TEST_CHARACTER)).thenReturn(simulatedCharacterCount);
        when(mockTribu.getPrestigePoints()).thenReturn(currentTotalPrestige);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: Verify points were added
        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestigePoints);

        // Assert: Verify outcome is correctly populated (not empty)
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertFalse(outcome.isEmpty(), "EffectOutcome should contain a ResourceDelta when points are awarded");
    }

    /**
     * Tests the early exit condition where the phase is END_GAME but the player
     * has 0 characters of the required type, resulting in a 0 bonus.
     */
    @Test
    void onPhaseChange_endGamePhaseWithZeroBonus_doesNothingAndReturnsEmpty() {
        // Arrange
        int simulatedCharacterCount = 0;

        when(mockTribu.getCharacterCount(TEST_CHARACTER)).thenReturn(simulatedCharacterCount);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: Verify no points were mistakenly added
        verify(mockTribu, never()).addPrestigePoints(anyInt());

        // Assert: Verify the outcome is explicitly empty
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty when bonus is 0");
    }

    /**
     * Tests that the effect completely ignores any phase change that is not END_GAME,
     * including null phases.
     * * @param phase A phase type that is NOT END_GAME, or null.
     */
    @ParameterizedTest
    @EnumSource(value = PhaseType.class, mode = EnumSource.Mode.EXCLUDE, names = {"END_GAME"})
    @NullSource
    void onPhaseChange_nonEndGameOrNullPhase_doesNothingAndReturnsEmpty(PhaseType phase) {
        // Act
        EffectOutcome outcome = effect.onPhaseChange(phase);

        // Assert: Verify the effect didn't even attempt to read the character count or add points
        verify(mockPlayer, never()).getTribu();
        verify(mockTribu, never()).getCharacterCount(any());
        verify(mockTribu, never()).addPrestigePoints(anyInt());

        // Assert: Outcome must be empty
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty for ignored phases");
    }
}