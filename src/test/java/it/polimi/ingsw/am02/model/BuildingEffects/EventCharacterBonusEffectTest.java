package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.EventType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class EventCharacterBonusEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private EventCharacterBonusEffect effect;

    // Mock constants
    // Sostituisci EVENT_MATCH e EVENT_MISMATCH con valori reali del tuo Enum EventType
    private final EventType EVENT_MATCH = EventType.SHAMANIC_RITUAL;
    private final EventType EVENT_MISMATCH = EventType.HUNT;
    private final CharacterType TEST_CHARACTER = CharacterType.GATHERER;

    private final int FOOD_REWARD = 2;
    private final int PRESTIGE_REWARD = 3;
    private final int FOOD_DISCOUNT = 1;

    @BeforeEach
    void setUp() {
        // Mock creation
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Effect instantiation
        effect = new EventCharacterBonusEffect(
                EVENT_MATCH,
                TEST_CHARACTER,
                FOOD_REWARD,
                PRESTIGE_REWARD,
                FOOD_DISCOUNT,
                mockTribu
        );
    }

    @Test
    void testAcceptCallsVisitEventObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitEventObserver(effect);
    }

    @Test
    void testEventStart_WithMatchingEvent_ShouldAddRewards() {
        // Explicit math setup
        int simulatedCharacterCount = 3;
        int expectedFood = FOOD_REWARD * simulatedCharacterCount;
        int expectedPrestige = PRESTIGE_REWARD * simulatedCharacterCount;
        int expectedDiscount = FOOD_DISCOUNT * simulatedCharacterCount;

        when(mockTribu.getCharacterCount(TEST_CHARACTER)).thenReturn(simulatedCharacterCount);

        // Triggering effect start
        effect.EventStart(EVENT_MATCH);

        // Verify all three resources are added correctly
        verify(mockTribu, times(1)).addFoodPoints(expectedFood);
        verify(mockTribu, times(1)).addPrestigePoints(expectedPrestige);
        verify(mockTribu, times(1)).addFoodDiscount(expectedDiscount);
    }

    @Test
    void testEventStart_WithZeroCharacters_ShouldAddZeroRewards() {
        // Edge Case setup
        int simulatedCharacterCount = 0;
        int expectedZero = 0;

        when(mockTribu.getCharacterCount(TEST_CHARACTER)).thenReturn(simulatedCharacterCount);

        // Triggering effect start
        effect.EventStart(EVENT_MATCH);

        // Verify zero is added to all resources without crashing
        verify(mockTribu, times(1)).addFoodPoints(expectedZero);
        verify(mockTribu, times(1)).addPrestigePoints(expectedZero);
        verify(mockTribu, times(1)).addFoodDiscount(expectedZero);
    }

    @Test
    void testEventStart_WithNonMatchingEvent_ShouldDoNothing() {
        // Trigger with the wrong event type
        effect.EventStart(EVENT_MISMATCH);

        // Verify Tribu is completely untouched
        verify(mockTribu, never()).getCharacterCount(any());
        verify(mockTribu, never()).addFoodPoints(anyInt());
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        verify(mockTribu, never()).addFoodDiscount(anyInt());
    }


    @Test
    void testEventEnd_WithMatchingEvent_ShouldRemoveDiscount() {
        // Current logic expects exactly the base discount (as a negative value)
        int expectedDiscountRemoval = -FOOD_DISCOUNT;

        // Triggering effect end
        effect.EventEnd(EVENT_MATCH);

        // Verify discount is removed
        verify(mockTribu, times(1)).addFoodDiscount(expectedDiscountRemoval);

        // Ensure food and prestige are NOT accidentally touched during EventEnd
        verify(mockTribu, never()).addFoodPoints(anyInt());
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }

    @Test
    void testEventEnd_WithNonMatchingEvent_ShouldDoNothing() {
        // Trigger with the wrong event type
        effect.EventEnd(EVENT_MISMATCH);

        // Verify Tribu is completely untouched
        verify(mockTribu, never()).addFoodDiscount(anyInt());
    }
}