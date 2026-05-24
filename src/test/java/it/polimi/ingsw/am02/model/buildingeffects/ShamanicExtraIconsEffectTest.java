package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
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
 * Unit tests for the {@link ShamanicExtraIconsEffect} class.
 * Ensures the effect temporarily grants bonus Shaman Stars to the player's Tribu
 * strictly during the SHAMANIC_RITUAL event and successfully revokes them at the end.
 */
class ShamanicExtraIconsEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private ShamanicExtraIconsEffect effect;

    // Strict test constants
    private static final int BONUS_STARS = 2;
    private static final String PLAYER_NICKNAME = "GammaPlayer";
    private static final EventType TARGET_EVENT = EventType.SHAMANIC_RITUAL;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Standard mock links
        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        // Instantiate the effect
        effect = new ShamanicExtraIconsEffect(mockPlayer, BONUS_STARS);
    }

    /**
     * Tests that the visitor pattern is correctly implemented.
     */
    @Test
    void accept_validVisitor_callsVisitEventObserver() {
        // Act
        effect.accept(mockVisitor);

        // Assert
        verify(mockVisitor, times(1)).visitEventObserver(effect);
    }

    // =========================================================================================
    // EVENT START TESTS
    // =========================================================================================

    /**
     * Tests that the effect adds the correct number of Shaman Stars and returns
     * a fully populated DTO when the triggering event starts.
     */
    @Test
    void eventStart_shamanicRitualEvent_addsTemporaryStarsAndReturnsDelta() {
        // Arrange
        int simulatedTotalStars = 5;
        when(mockTribu.getShamanStars()).thenReturn(simulatedTotalStars);

        // Act
        EffectOutcome outcome = effect.eventStart(TARGET_EVENT);

        // Assert: Interactions
        verify(mockTribu, times(1)).addShamanStars(BONUS_STARS);

        // Assert: Outcome state
        assertNotNull(outcome);
        assertFalse(outcome.isEmpty());
        assertEquals(1, outcome.resourceDeltas().size());

        // Assert: Delta properties
        assertEquals(ResourceType.SHAMAN_STARS, outcome.resourceDeltas().get(0).resource());
        assertEquals(BONUS_STARS, outcome.resourceDeltas().get(0).delta());
    }

    /**
     * Tests that the effect ignores all other events starting, including null events.
     */
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SHAMANIC_RITUAL"})
    @NullSource
    void eventStart_nonShamanicOrNullEvent_doesNothingAndReturnsEmpty(EventType eventType) {
        // Act
        EffectOutcome outcome = effect.eventStart(eventType);

        // Assert
        verify(mockPlayer, never()).getTribu();
        verify(mockTribu, never()).addShamanStars(anyInt());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }

    // =========================================================================================
    // EVENT END TESTS
    // =========================================================================================

    /**
     * Tests that the effect correctly subtracts the previously added Shaman Stars
     * (by passing a negative delta) and returns an accurate DTO when the event ends.
     */
    @Test
    void eventEnd_shamanicRitualEvent_removesTemporaryStarsAndReturnsDelta() {
        // Arrange
        int simulatedTotalStars = 3; // Stars left after subtraction
        int expectedRemoval = -BONUS_STARS;
        when(mockTribu.getShamanStars()).thenReturn(simulatedTotalStars);

        // Act
        EffectOutcome outcome = effect.eventEnd(TARGET_EVENT);

        // Assert: Interactions
        verify(mockTribu, times(1)).addShamanStars(expectedRemoval);

        // Assert: Outcome state
        assertNotNull(outcome);
        assertFalse(outcome.isEmpty());
        assertEquals(1, outcome.resourceDeltas().size());

        // Assert: Delta properties
        assertEquals(ResourceType.SHAMAN_STARS, outcome.resourceDeltas().get(0).resource());
        assertEquals(expectedRemoval, outcome.resourceDeltas().get(0).delta());
    }

    /**
     * Tests that the effect ignores all other events ending, avoiding accidental
     * subtraction of stars.
     */
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SHAMANIC_RITUAL"})
    @NullSource
    void eventEnd_nonShamanicOrNullEvent_doesNothingAndReturnsEmpty(EventType eventType) {
        // Act
        EffectOutcome outcome = effect.eventEnd(eventType);

        // Assert
        verify(mockPlayer, never()).getTribu();
        verify(mockTribu, never()).addShamanStars(anyInt());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }
}