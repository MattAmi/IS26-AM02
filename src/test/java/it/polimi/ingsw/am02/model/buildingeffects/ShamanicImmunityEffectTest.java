package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ShamanicImmunityEffect} class.
 * Ensures that the effect properly grants and revokes immunity to the player's Tribu
 * exclusively during the SHAMANIC_RITUAL event, and correctly implements the visitor pattern.
 */
class ShamanicImmunityEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private ShamanicImmunityEffect effect;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks and the effect instance before each test
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        when(mockPlayer.getTribu()).thenReturn(mockTribu);

        effect = new ShamanicImmunityEffect(mockPlayer);
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
     * Tests that immunity is granted when the triggering event starts.
     */
    @Test
    void eventStart_shamanicRitualEvent_setsImmunityToTrue() {
        // Act
        EffectOutcome outcome = effect.eventStart(EventType.SHAMANIC_RITUAL);

        // Assert: Verify state change on Tribu and the correctness of the returned outcome
        verify(mockTribu, times(1)).setImmuneToShamanicPenalty(true);
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty for this effect");
    }

    /**
     * Tests that the effect remains inactive for any event other than SHAMANIC_RITUAL,
     * including null events.
     * * @param eventType An event type that is NOT SHAMANIC_RITUAL, or null.
     */
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SHAMANIC_RITUAL"})
    @NullSource
    void eventStart_nonTriggeringOrNullEvent_doesNotChangeImmunity(EventType eventType) {
        // Act
        EffectOutcome outcome = effect.eventStart(eventType);

        // Assert: Verify no interactions occurred with the Tribu's immunity setter
        verify(mockTribu, never()).setImmuneToShamanicPenalty(anyBoolean());
        assertNotNull(outcome, "EffectOutcome should not be null even for ignored events");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty");
    }

    // =========================================================================================
    // EVENT END TESTS
    // =========================================================================================

    /**
     * Tests that immunity is correctly revoked when the triggering event ends.
     */
    @Test
    void eventEnd_shamanicRitualEvent_setsImmunityToFalse() {
        // Act
        EffectOutcome outcome = effect.eventEnd(EventType.SHAMANIC_RITUAL);

        // Assert: Verify state change on Tribu and the correctness of the returned outcome
        verify(mockTribu, times(1)).setImmuneToShamanicPenalty(false);
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty for this effect");
    }

    /**
     * Tests that the effect does not accidentally revoke immunity when other events end,
     * including null events.
     * * @param eventType An event type that is NOT SHAMANIC_RITUAL, or null.
     */
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SHAMANIC_RITUAL"})
    @NullSource
    void eventEnd_nonTriggeringOrNullEvent_doesNotChangeImmunity(EventType eventType) {
        // Act
        EffectOutcome outcome = effect.eventEnd(eventType);

        // Assert: Verify no interactions occurred with the Tribu's immunity setter
        verify(mockTribu, never()).setImmuneToShamanicPenalty(anyBoolean());
        assertNotNull(outcome, "EffectOutcome should not be null even for ignored events");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty");
    }
}