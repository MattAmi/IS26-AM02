package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link FlatPrestigeBonusEffect} class.
 * Ensures that the effect grants a fixed amount of prestige points to the player
 * exclusively during the END_GAME phase, and generates the correct ResourceDelta.
 */
class FlatPrestigeBonusEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private FlatPrestigeBonusEffect effect;

    // Strict test constants
    private static final int FLAT_BONUS_PP = 5;
    private static final String TEST_NICKNAME = "DeltaPlayer";

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Common mock behaviors
        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(TEST_NICKNAME);

        // Instantiate the effect
        effect = new FlatPrestigeBonusEffect(mockPlayer, FLAT_BONUS_PP);
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
     * Tests that the effect adds the exact flat bonus amount and returns a correctly
     * populated EffectOutcome when the phase is END_GAME.
     */
    @Test
    void onPhaseChange_endGamePhase_addsExactBonusAndReturnsDelta() {
        // Arrange
        int simulatedTotalPrestige = 15;
        when(mockTribu.getPrestigePoints()).thenReturn(simulatedTotalPrestige);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: Verify interactions with the Tribu
        verify(mockTribu, times(1)).addPrestigePoints(FLAT_BONUS_PP);

        // Assert: Verify outcome payload
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertFalse(outcome.isEmpty(), "EffectOutcome should contain a ResourceDelta");
        assertEquals(1, outcome.resourceDeltas().size(), "There should be exactly one delta");

        // Assert: Verify delta contents using record accessors
        assertEquals(ResourceType.PRESTIGE_POINTS, outcome.resourceDeltas().get(0).resource(), "Delta should be for PRESTIGE_POINTS");
        assertEquals(FLAT_BONUS_PP, outcome.resourceDeltas().get(0).delta(), "Delta amount should match the flat bonus");
    }

    /**
     * Tests that the effect remains completely dormant during any phase other than END_GAME,
     * including null phases, and returns an empty outcome.
     * @param phase A phase type that is NOT END_GAME, or null.
     */
    @ParameterizedTest
    @EnumSource(value = PhaseType.class, mode = EnumSource.Mode.EXCLUDE, names = {"END_GAME"})
    @NullSource
    void onPhaseChange_nonEndGameOrNullPhase_doesNothingAndReturnsEmpty(PhaseType phase) {
        // Act
        EffectOutcome outcome = effect.onPhaseChange(phase);

        // Assert: The effect must remain completely silent
        verify(mockPlayer, never()).getTribu();
        verify(mockTribu, never()).addPrestigePoints(anyInt());

        // Assert: Outcome must be empty
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be explicitly empty when phase is ignored");
    }
}