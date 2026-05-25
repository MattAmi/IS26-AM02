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
 * Unit tests for the {@link EndGameFullSetPPEffect} class.
 * Ensures that the effect calculates the minimum number of character sets
 * across all CharacterTypes, and awards the correct multiplier of prestige points
 * exclusively during the END_GAME phase.
 */
class EndGameFullSetPPEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private EndGameFullSetPPEffect effect;

    // Constants for test predictability
    private static final int BONUS_PP_PER_SET = 3;
    private static final String PLAYER_NICKNAME = "BetaPlayer";

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Setup common mock behaviors
        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        // Instantiate the effect
        effect = new EndGameFullSetPPEffect(mockPlayer, BONUS_PP_PER_SET);
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
     * Tests calculation and DTO generation when the player has perfectly balanced sets
     * of every character type.
     */
    @Test
    void onPhaseChange_endGamePhaseWithPerfectSets_calculatesCorrectBonusAndReturnsDelta() {
        // Arrange
        int simulatedCharacterCount = 2; // 2 of EVERY character
        int expectedPrestigePoints = BONUS_PP_PER_SET * simulatedCharacterCount; // 3 * 2 = 6

        // any() tells Mockito to return 2 no matter which CharacterType is asked for
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(simulatedCharacterCount);
        when(mockTribu.getPrestigePoints()).thenReturn(20);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert
        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestigePoints);
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertFalse(outcome.isEmpty(), "EffectOutcome should contain a ResourceDelta when points are awarded");
    }

    /**
     * Tests the calculation logic to ensure it properly identifies the bottleneck
     * (the character type with the lowest count) when determining the number of full sets.
     */
    @Test
    void onPhaseChange_endGamePhaseWithUnevenSets_usesBottleneckToCalculateBonus() {
        // Arrange
        int baseCharacterCount = 3;
        int bottleneckCount = 1; // Only 1 set can be completed
        int expectedPrestigePoints = BONUS_PP_PER_SET * bottleneckCount; // 3 * 1 = 3

        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(baseCharacterCount);
        when(mockTribu.getCharacterCount(CharacterType.SHAMAN)).thenReturn(bottleneckCount);
        when(mockTribu.getPrestigePoints()).thenReturn(20);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert
        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestigePoints);
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertFalse(outcome.isEmpty(), "EffectOutcome should contain a ResourceDelta");
    }

    /**
     * Tests the early exit logic when the player is missing at least one character type entirely,
     * resulting in 0 completed sets.
     */
    @Test
    void onPhaseChange_endGamePhaseWithMissingCharacter_addsZeroPointsAndReturnsEmpty() {
        // Arrange
        int baseCharacterCount = 5;
        int bottleneckCount = 0; // 0 sets completed

        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(baseCharacterCount);
        when(mockTribu.getCharacterCount(CharacterType.SHAMAN)).thenReturn(bottleneckCount);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: Verify it explicitly avoids adding 0 points to the Tribu
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be explicitly empty when bonus is 0");
    }

    /**
     * Tests that the effect completely ignores any phase change that is not END_GAME.
     * @param phase A phase type that is NOT END_GAME, or null.
     */
    @ParameterizedTest
    @EnumSource(value = PhaseType.class, mode = EnumSource.Mode.EXCLUDE, names = {"END_GAME"})
    @NullSource
    void onPhaseChange_nonEndGameOrNullPhase_doesNothingAndReturnsEmpty(PhaseType phase) {
        // Act
        EffectOutcome outcome = effect.onPhaseChange(phase);

        // Assert: Verify no logic was triggered
        verify(mockPlayer, never()).getTribu();
        verify(mockTribu, never()).getCharacterCount(any());
        verify(mockTribu, never()).addPrestigePoints(anyInt());

        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty for ignored phases");
    }
}