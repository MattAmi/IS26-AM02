package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.enumerations.EventType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class ShamanicExtraIconsEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private ShamanicExtraIconsEffect effect;

    // Strict test constants (No Magic Numbers)
    private final int BONUS_STARS = 2;
    private final EventType TRIGGERING_EVENT = EventType.SHAMANIC_RITUAL;

    // We dynamically pick an event that is strictly NOT a SHAMANIC_RITUAL to test the mismatch
    private final EventType NON_TRIGGERING_EVENT = getAlternativeEvent();

    @BeforeEach
    void setUp() {
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);
        effect = new ShamanicExtraIconsEffect(mockTribu, BONUS_STARS);
    }

    @Test
    void testAcceptCallsVisitEventObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitEventObserver(effect);
    }

    // --- EVENT START TESTS ---

    @Test
    void testEventStart_WithShamanicRitual_ShouldAddTemporaryStars() {
        // Act
        effect.EventStart(TRIGGERING_EVENT);

        // Assert: Explicitly verify the exact bonus amount is added
        verify(mockTribu, times(1)).addShamanStars(BONUS_STARS);
    }

    @Test
    void testEventStart_WithDifferentEvent_ShouldDoNothing() {
        // Act: Trigger an event like DROUGHT or FAMINE
        effect.EventStart(NON_TRIGGERING_EVENT);

        // Assert: The effect must remain completely silent
        verify(mockTribu, never()).addShamanStars(anyInt());
    }

    // --- EVENT END TESTS ---

    @Test
    void testEventEnd_WithShamanicRitual_ShouldRemoveTemporaryStars() {
        // Act
        effect.EventEnd(TRIGGERING_EVENT);

        // Assert: Explicitly verify the exact bonus amount is SUBTRACTED (added as negative)
        // This ensures the player doesn't keep the stars forever
        int expectedRemoval = -BONUS_STARS;
        verify(mockTribu, times(1)).addShamanStars(expectedRemoval);
    }

    @Test
    void testEventEnd_WithDifferentEvent_ShouldDoNothing() {
        // Act
        effect.EventEnd(NON_TRIGGERING_EVENT);

        // Assert: Ensure it doesn't accidentally subtract stars during the wrong event cleanup
        verify(mockTribu, never()).addShamanStars(anyInt());
    }

    // --- HELPER METHOD ---

    /**
     * Safely grabs an EventType that is NOT SHAMANIC_RITUAL to avoid hardcoding
     * a specific second event that your team might rename or delete later.
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