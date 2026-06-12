package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.card.EventCard;
import it.polimi.ingsw.am02.server.model.effect.EventEffect;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests {@link EventCard}, verifying its accessors and that resolving an event
 * combines the card's own effect with the deltas contributed by registered
 * {@link EventObserver}s.
 */
class EventCardTest {

    /** Verifies the event card exposes the properties it was constructed with. */
    @Test
    void testEventCardProperties() {
        EventEffect mockEffect = Mockito.mock(EventEffect.class);
        EventCard card = new EventCard("E_001", Era.I, EventType.HUNT, false, 1, mockEffect);

        assertEquals("E_001", card.getID());
        assertEquals(Era.I, card.getEra());
        assertEquals(EventType.HUNT, card.getType());
        assertFalse(card.isFinal());
        assertEquals(1, card.getPriority());
    }

    /** Verifies that the event effect and observers' post-resolution deltas are merged. */
    @Test
    void testApplyEventEffectWithObservers() {
        EventEffect mockEffect = Mockito.mock(EventEffect.class);
        when(mockEffect.applyEffect(any())).thenReturn(
                new EffectOutcome(List.of(new ResourceDelta("Alice", ResourceType.FOOD, 5, 2)))
        );

        EventCard card = new EventCard("E_001", Era.I, EventType.HUNT, false, 1, mockEffect);

        EventObserver mockObserver = Mockito.mock(EventObserver.class);
        when(mockObserver.eventPostResolution(EventType.HUNT)).thenReturn(
                new EffectOutcome(List.of(new ResourceDelta("Alice", ResourceType.PRESTIGE_POINTS, 10, 5)))
        );

        EffectOutcome outcome = card.applyEventEffect(List.of(), List.of(mockObserver));

        assertEquals(2, outcome.resourceDeltas().size());
        assertEquals(ResourceType.FOOD, outcome.resourceDeltas().get(0).resource());
        assertEquals(ResourceType.PRESTIGE_POINTS, outcome.resourceDeltas().get(1).resource());
    }
}
