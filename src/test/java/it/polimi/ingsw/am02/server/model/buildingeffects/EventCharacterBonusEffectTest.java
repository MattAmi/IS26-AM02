package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link EventCharacterBonusEffect} class.
 * Ensures the correct calculation of food, prestige, and food discount rewards based on character count
 * at the start of a specific event, and ensures discounts are correctly reverted at the end of the event.
 */
class EventCharacterBonusEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private EventCharacterBonusEffect effect;

    // Standard Test Constants
    private static final EventType TARGET_EVENT = EventType.SHAMANIC_RITUAL;
    private static final CharacterType TARGET_CHARACTER = CharacterType.GATHERER;
    private static final String PLAYER_NICKNAME = "CharliePlayer";

    private static final int FOOD_REWARD = 2;
    private static final int PRESTIGE_REWARD = 3;
    private static final int FOOD_DISCOUNT = 1;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        // Standard instance for most tests
        effect = new EventCharacterBonusEffect(
                mockPlayer, TARGET_EVENT, TARGET_CHARACTER,
                FOOD_REWARD, PRESTIGE_REWARD, FOOD_DISCOUNT
        );
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
     * Tests that all rewards are calculated properly and applied when the event matches
     * and the player has the required characters. Also verifies the outcome contains 3 deltas.
     */
    @Test
    void eventStart_matchingEventWithCharacters_addsAllRewardsAndReturnsDeltas() {
        // Arrange
        int count = 3;
        int expFood = FOOD_REWARD * count;
        int expPP = PRESTIGE_REWARD * count;
        int expDiscount = FOOD_DISCOUNT * count;

        when(mockTribu.getCharacterCount(TARGET_CHARACTER)).thenReturn(count);
        when(mockTribu.getFoodPoints()).thenReturn(10);
        when(mockTribu.getPrestigePoints()).thenReturn(15);
        when(mockTribu.getFoodDiscount()).thenReturn(expDiscount);

        // Act
        EffectOutcome outcome = effect.eventStart(TARGET_EVENT);

        // Assert: Interactions
        verify(mockTribu, times(1)).addFoodPoints(expFood);
        verify(mockTribu, times(1)).addPrestigePoints(expPP);
        verify(mockTribu, times(1)).addFoodDiscount(expDiscount);

        // Assert: Outcome state
        assertNotNull(outcome, "Outcome must not be null");
        assertFalse(outcome.isEmpty(), "Outcome must not be empty");

        // CORRETTO: Usa resourceDeltas() del record invece di getUpdatedResources()
        List<ResourceType> updatedResources = outcome.resourceDeltas().stream()
                .map(ResourceDelta::resource) // NOTA: Usa getResourceType() se ResourceDelta non è un record
                .collect(Collectors.toList());

        assertTrue(updatedResources.contains(ResourceType.FOOD));
        assertTrue(updatedResources.contains(ResourceType.PRESTIGE_POINTS));
        assertTrue(updatedResources.contains(ResourceType.FOOD_DISCOUNT));
        assertEquals(3, outcome.resourceDeltas().size());
    }

    /**
     * Edge case: Verifies that if one of the rewards is set to 0 (e.g., this card only gives PP and discount, no food),
     * the system correctly skips adding 0 and does not generate a delta for that resource.
     */
    @Test
    void eventStart_matchingEventWithPartialRewards_addsOnlyPositiveRewards() {
        // Arrange: Recreate effect with 0 food reward
        EventCharacterBonusEffect partialEffect = new EventCharacterBonusEffect(
                mockPlayer, TARGET_EVENT, TARGET_CHARACTER, 0, PRESTIGE_REWARD, FOOD_DISCOUNT
        );

        when(mockTribu.getCharacterCount(TARGET_CHARACTER)).thenReturn(2);

        // Act
        EffectOutcome outcome = partialEffect.eventStart(TARGET_EVENT);

        // Assert
        verify(mockTribu, never()).addFoodPoints(anyInt());
        verify(mockTribu, times(1)).addPrestigePoints(PRESTIGE_REWARD * 2);
        verify(mockTribu, times(1)).addFoodDiscount(FOOD_DISCOUNT * 2);

        // CORRETTO: Usa resourceDeltas()
        assertEquals(2, outcome.resourceDeltas().size(), "Should only contain deltas for PP and Discount");
    }

    /**
     * Tests early exit when the player has 0 characters of the required type.
     */
    @Test
    void eventStart_matchingEventWithZeroCharacters_doesNothingAndReturnsEmpty() {
        // Arrange
        when(mockTribu.getCharacterCount(TARGET_CHARACTER)).thenReturn(0);

        // Act
        EffectOutcome outcome = effect.eventStart(TARGET_EVENT);

        // Assert
        verify(mockTribu, never()).addFoodPoints(anyInt());
        verify(mockTribu, never()).addPrestigePoints(anyInt());
        verify(mockTribu, never()).addFoodDiscount(anyInt());
        assertTrue(outcome.isEmpty(), "Outcome must be empty if character count is 0");
    }

    /**
     * Tests that the effect ignores all other events.
     * @param eventType An event type that is NOT the target event.
     */
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SHAMANIC_RITUAL"})
    @NullSource
    void eventStart_nonMatchingOrNullEvent_doesNothingAndReturnsEmpty(EventType eventType) {
        // Act
        EffectOutcome outcome = effect.eventStart(eventType);

        // Assert
        verify(mockPlayer, never()).getTribu();
        assertTrue(outcome.isEmpty());
    }

    // =========================================================================================
    // EVENT END TESTS
    // =========================================================================================

    /**
     * Tests that the discount granted at the start of the event is correctly removed
     * (subtracted) at the end of the event.
     */
    @Test
    void eventEnd_matchingEventWithCharacters_removesDiscountAndReturnsDelta() {
        // Arrange
        int count = 3;
        int expectedDiscountRemoval = -(FOOD_DISCOUNT * count); // Must be negative

        when(mockTribu.getCharacterCount(TARGET_CHARACTER)).thenReturn(count);
        when(mockTribu.getFoodDiscount()).thenReturn(0);

        // Act
        EffectOutcome outcome = effect.eventEnd(TARGET_EVENT);

        // Assert
        verify(mockTribu, times(1)).addFoodDiscount(expectedDiscountRemoval);

        // Ensure it doesn't touch other resources
        verify(mockTribu, never()).addFoodPoints(anyInt());
        verify(mockTribu, never()).addPrestigePoints(anyInt());

        assertFalse(outcome.isEmpty());
        // CORRETTO: Usa resourceDeltas()
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(ResourceType.FOOD_DISCOUNT, outcome.resourceDeltas().get(0).resource());
        assertEquals(expectedDiscountRemoval, outcome.resourceDeltas().get(0).delta()); // NOTA: usa getAmount() se ResourceDelta non è un record e adattalo se il delta si chiama diversamente
    }

    /**
     * Tests early exit on EventEnd if the player has 0 characters.
     */
    @Test
    void eventEnd_matchingEventWithZeroCharacters_doesNothingAndReturnsEmpty() {
        // Arrange
        when(mockTribu.getCharacterCount(TARGET_CHARACTER)).thenReturn(0);

        // Act
        EffectOutcome outcome = effect.eventEnd(TARGET_EVENT);

        // Assert
        verify(mockTribu, never()).addFoodDiscount(anyInt());
        assertTrue(outcome.isEmpty());
    }

    /**
     * Tests that the effect ignores all other events ending.
     * @param eventType An event type that is NOT the target event.
     */
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SHAMANIC_RITUAL"})
    @NullSource
    void eventEnd_nonMatchingOrNullEvent_doesNothingAndReturnsEmpty(EventType eventType) {
        // Act
        EffectOutcome outcome = effect.eventEnd(eventType);

        // Assert
        verify(mockPlayer, never()).getTribu();
        assertTrue(outcome.isEmpty());
    }
}