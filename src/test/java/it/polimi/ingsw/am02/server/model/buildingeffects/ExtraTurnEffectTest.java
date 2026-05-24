package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ExtraTurnEffect} class.
 * Ensures that the effect correctly enqueues an extra turn for the player
 * exclusively during the END_ROUND phase, and properly implements the visitor pattern.
 */
class ExtraTurnEffectTest {

    private Player mockPlayer;
    private Game mockGame;
    private EffectVisitor mockVisitor;
    private ExtraTurnEffect effect;

    // Strict test constants to prevent false positives
    private static final String TEST_NICKNAME = "TestPlayer123";
    private static final int EXTRA_UPPER_PICKS = 2;
    private static final int EXTRA_LOWER_PICKS = 1;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockGame = Mockito.mock(Game.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Standard setup for the player mock
        when(mockPlayer.getNickname()).thenReturn(TEST_NICKNAME);

        // Instantiate the effect with strict expected values
        effect = new ExtraTurnEffect(mockPlayer, mockGame, EXTRA_UPPER_PICKS, EXTRA_LOWER_PICKS);
    }

    /**
     * Tests that the visitor pattern is correctly implemented.
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
     * Tests that the effect extracts the player's nickname and enqueues an extra turn
     * with the precise configuration when the END_ROUND phase triggers.
     */
    @Test
    void onPhaseChange_endRoundPhase_enqueuesExtraTurnAndReturnsEmptyOutcome() {
        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_ROUND);

        // Assert: Verify state and interactions strictly
        verify(mockPlayer, times(1)).getNickname();
        verify(mockGame, times(1)).enqueueExtraTurn(TEST_NICKNAME, EXTRA_UPPER_PICKS, EXTRA_LOWER_PICKS);

        // Assert: Ensure the outcome obeys the contract
        assertNotNull(outcome, "EffectOutcome should never be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be explicitly empty for this effect");
    }

    /**
     * Tests that the effect remains completely dormant during any phase other than END_ROUND,
     * including null phases.
     * @param phase A phase type that is NOT END_ROUND, or null.
     */
    @ParameterizedTest
    @EnumSource(value = PhaseType.class, mode = EnumSource.Mode.EXCLUDE, names = {"END_ROUND"})
    @NullSource
    void onPhaseChange_nonEndRoundOrNullPhase_doesNothingAndReturnsEmptyOutcome(PhaseType phase) {
        // Act
        EffectOutcome outcome = effect.onPhaseChange(phase);

        // Assert: Verify the effect remained silent
        verify(mockPlayer, never()).getNickname();
        verify(mockGame, never()).enqueueExtraTurn(anyString(), anyInt(), anyInt());

        // Assert: Ensure the outcome obeys the contract even when ignored
        assertNotNull(outcome, "EffectOutcome should never be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be explicitly empty when phase is ignored");
    }
}