package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ShamanicWinMultiplierEffect} class.
 * Ensures the effect correctly multiplies the prestige points won during a SHAMANIC_RITUAL,
 * explicitly subtracting the base points already received (multiplier - 1).
 */
class ShamanicWinMultiplierEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private ShamanicWinMultiplierEffect effect;

    // Strict test constants
    private static final int MULTIPLIER = 3;
    private static final int BASE_BONUS_RECEIVED = 5;
    private static final String PLAYER_NICKNAME = "OmegaPlayer";
    private static final EventType TARGET_EVENT = EventType.SHAMANIC_RITUAL;

    @BeforeEach
    void setUp() {
        // Arrange
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        effect = new ShamanicWinMultiplierEffect(mockPlayer, MULTIPLIER);
    }

    @Test
    void accept_validVisitor_callsVisitEventObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitEventObserver(effect);
    }

    // =========================================================================================
    // EVENT POST RESOLUTION TESTS
    // =========================================================================================

    /**
     * Tests that the extra bonus is correctly calculated and applied when the player
     * actually received a base bonus from winning the ritual.
     */
    @Test
    void eventPostResolution_shamanicRitualWithPositiveBonus_appliesMultiplierAndReturnsDelta() {
        // Arrange
        when(mockTribu.getLastEventBonusReceived()).thenReturn(BASE_BONUS_RECEIVED);
        when(mockTribu.getPrestigePoints()).thenReturn(20);

        // Expected math: 5 * (3 - 1) = 10 extra points
        int expectedExtraBonus = BASE_BONUS_RECEIVED * (MULTIPLIER - 1);

        // Act
        EffectOutcome outcome = effect.eventPostResolution(TARGET_EVENT);

        // Assert: Interactions
        verify(mockTribu, times(1)).addPrestigePoints(expectedExtraBonus);

        // Assert: Outcome state
        assertNotNull(outcome);
        assertFalse(outcome.isEmpty());
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(ResourceType.PRESTIGE_POINTS, outcome.resourceDeltas().get(0).resource());
        assertEquals(expectedExtraBonus, outcome.resourceDeltas().get(0).delta());
    }

    /**
     * Tests that no points are added and an empty outcome is returned if the player
     * received 0 points from the event (e.g., they lost the ritual).
     */
    @Test
    void eventPostResolution_shamanicRitualWithZeroBonus_doesNothingAndReturnsEmpty() {
        // Arrange
        when(mockTribu.getLastEventBonusReceived()).thenReturn(0);

        // Act
        EffectOutcome outcome = effect.eventPostResolution(TARGET_EVENT);

        // Assert
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }

    /**
     * Tests that the effect ignores all other events post-resolution.
     */
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SHAMANIC_RITUAL"})
    @NullSource
    void eventPostResolution_nonShamanicOrNullEvent_doesNothingAndReturnsEmpty(EventType eventType) {
        // Act
        EffectOutcome outcome = effect.eventPostResolution(eventType);

        // Assert
        verify(mockPlayer, never()).getTribu();
        verify(mockTribu, never()).getLastEventBonusReceived();
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }
}