package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.EventType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class ShamanicImmunityEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private ShamanicImmunityEffect effect;

    // Strict test constants
    private final EventType TRIGGERING_EVENT = EventType.SHAMANIC_RITUAL;
    private final EventType NON_TRIGGERING_EVENT = getAlternativeEvent();

    @BeforeEach
    void setUp() {
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);
        effect = new ShamanicImmunityEffect(mockTribu);
    }

    @Test
    void testAcceptCallsVisitEventObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitEventObserver(effect);
    }

    // --- EVENT START TESTS ---

    @Test
    void testEventStart_WithShamanicRitual_ShouldGrantImmunity() {
        // Act
        effect.EventStart(TRIGGERING_EVENT);

        // Assert: Explicitly verify the immunity is turned ON (true) using corrected method name
        verify(mockTribu, times(1)).setImmuneToShamanicPenalty(true);
    }

    @Test
    void testEventStart_WithDifferentEvent_ShouldDoNothing() {
        // Act
        effect.EventStart(NON_TRIGGERING_EVENT);

        // Assert: The effect must remain completely silent
        verify(mockTribu, never()).setImmuneToShamanicPenalty(anyBoolean());
    }

    // --- EVENT END TESTS ---

    @Test
    void testEventEnd_WithShamanicRitual_ShouldRevokeImmunity() {
        // Act
        effect.EventEnd(TRIGGERING_EVENT);

        // Assert: Explicitly verify the immunity is turned OFF (false) using corrected method name
        verify(mockTribu, times(1)).setImmuneToShamanicPenalty(false);
    }

    @Test
    void testEventEnd_WithDifferentEvent_ShouldDoNothing() {
        // Act
        effect.EventEnd(NON_TRIGGERING_EVENT);

        // Assert: Ensure it doesn't accidentally revoke immunity during the wrong event cleanup
        verify(mockTribu, never()).setImmuneToShamanicPenalty(anyBoolean());
    }

    // --- HELPER METHOD ---

    /**
     * Safely grabs an EventType that is NOT SHAMANIC_RITUAL to avoid hardcoding
     * a specific alternative event that could be renamed or removed later.
     */
    private EventType getAlternativeEvent() {
        for (EventType type : EventType.values()) {
            if (type != EventType.SHAMANIC_RITUAL) {
                return type;
            }
        }
        throw new IllegalStateException("EventType enum needs at least one other event besides SHAMANIC_RITUAL for testing!");
    }
}