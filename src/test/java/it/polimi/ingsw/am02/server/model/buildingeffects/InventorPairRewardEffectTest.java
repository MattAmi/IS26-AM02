package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link InventorPairRewardEffect} class.
 * Ensures the effect correctly tracks pairs of inventions (2 of the same type)
 * and rewards the player exclusively when a new pair is formed upon placing an INVENTOR,
 * accurately preventing retroactive rewards.
 */
class InventorPairRewardEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;

    // Strict test constants
    private static final int FOOD_REWARD_PER_PAIR = 3;
    private static final String PLAYER_NICKNAME = "FoxtrotPlayer";
    private static final CharacterType TRIGGERING_CHARACTER = CharacterType.INVENTOR;

    // Safe extraction of enum values to prevent hardcoding specific names
    private static final InventionType INVENTION_A = InventionType.values()[0];
    private static final InventionType INVENTION_B = InventionType.values()[1];

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Standard mock linking
        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        // Default state: The player starts with 0 of every invention
        when(mockTribu.getInventionTypeCount(any(InventionType.class))).thenReturn(0);
    }

    /**
     * Tests that the visitor pattern is correctly implemented.
     */
    @Test
    void accept_validVisitor_callsVisitTribuObserver() {
        // Arrange
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockPlayer, FOOD_REWARD_PER_PAIR);

        // Act
        effect.accept(mockVisitor);

        // Assert
        verify(mockVisitor, times(1)).visitTribuObserver(effect);
    }

    // =========================================================================================
    // ON CHARACTER INSERTION TESTS
    // =========================================================================================

    /**
     * Tests that the effect ignores the placement of any character that is NOT an INVENTOR,
     * including null character placements.
     * @param characterType A character type that is NOT INVENTOR, or null.
     */
    @ParameterizedTest
    @EnumSource(value = CharacterType.class, mode = EnumSource.Mode.EXCLUDE, names = {"INVENTOR"})
    @NullSource
    void onCharacterInsertion_nonInventorCharacter_doesNothingAndReturnsEmpty(CharacterType characterType) {
        // Arrange
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockPlayer, FOOD_REWARD_PER_PAIR);
        Mockito.clearInvocations(mockPlayer, mockTribu); // Reset after constructor

        // Act
        EffectOutcome outcome = effect.onCharacterInsertion(characterType);

        // Assert: Verify it never even asks the player for the Tribu
        verify(mockPlayer, never()).getTribu();
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be explicitly empty");
    }

    /**
     * Tests that no reward is given if an INVENTOR is played but it does not
     * complete a new pair of inventions (e.g., placing the 1st or 3rd invention of a type).
     */
    @Test
    void onCharacterInsertion_inventorPlayedButNoNewPairCompleted_doesNothingAndReturnsEmpty() {
        // Arrange
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockPlayer, FOOD_REWARD_PER_PAIR);

        // Fast-forward state: Player gets 1 invention of type A. (1 / 2 = 0 pairs)
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(1);

        // Act
        EffectOutcome outcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert
        verify(mockTribu, never()).addFoodPoints(anyInt());
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty");
    }

    /**
     * Tests that the player receives exactly one reward multiplier and a properly populated DTO
     * when a single new pair of inventions is completed.
     */
    @Test
    void onCharacterInsertion_singlePairCompleted_addsFoodAndReturnsDelta() {
        // Arrange
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockPlayer, FOOD_REWARD_PER_PAIR);

        // Fast-forward state: Player gets their 2nd invention of type A, completing exactly 1 pair.
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(2);
        when(mockTribu.getFoodPoints()).thenReturn(10); // Simulated total after addition

        // Act
        EffectOutcome outcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert
        verify(mockTribu, times(1)).addFoodPoints(FOOD_REWARD_PER_PAIR);

        assertNotNull(outcome, "EffectOutcome should not be null");
        assertFalse(outcome.isEmpty(), "EffectOutcome should contain a ResourceDelta");
        assertEquals(1, outcome.resourceDeltas().size(), "There should be exactly one delta");

        assertEquals(ResourceType.FOOD, outcome.resourceDeltas().get(0).resource(), "Delta should be for FOOD");
        assertEquals(FOOD_REWARD_PER_PAIR, outcome.resourceDeltas().get(0).delta(), "Delta amount should match the reward per pair");
    }

    /**
     * Tests the multiplier logic when a massive combo results in multiple pairs being
     * completed simultaneously.
     */
    @Test
    void onCharacterInsertion_multiplePairsCompletedAtOnce_addsMultipliedFoodAndReturnsDelta() {
        // Arrange
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockPlayer, FOOD_REWARD_PER_PAIR);

        // Fast-forward state: 2 new pairs are completed simultaneously (2 of A and 2 of B)
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(2);
        when(mockTribu.getInventionTypeCount(INVENTION_B)).thenReturn(2);
        when(mockTribu.getFoodPoints()).thenReturn(15);

        int expectedFood = FOOD_REWARD_PER_PAIR * 2; // 3 * 2 = 6

        // Act
        EffectOutcome outcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert
        verify(mockTribu, times(1)).addFoodPoints(expectedFood);

        assertNotNull(outcome);
        assertFalse(outcome.isEmpty());
        assertEquals(expectedFood, outcome.resourceDeltas().get(0).delta());
    }

    /**
     * Tests the constructor's ability to accurately map existing pairs to prevent
     * rewarding the player retroactively for pairs completed before acquiring the effect.
     */
    @Test
    void onCharacterInsertion_playerAlreadyHasPairs_doesNotRewardRetroactively() {
        // Arrange 1: Player ALREADY has 3 inventions of type A (which means 1 full pair exists)
        // BEFORE the building is constructed.
        int existingInventions = 3;
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(existingInventions);

        // The effect is created. It MUST memorize the existing 1 pair.
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockPlayer, FOOD_REWARD_PER_PAIR);

        // Arrange 2 & Act 1: Player inserts another Inventor, but it doesn't form a pair (still 3 inventions).
        EffectOutcome firstOutcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert 1: No food rewarded retroactively.
        verify(mockTribu, never()).addFoodPoints(anyInt());
        assertTrue(firstOutcome.isEmpty());

        // Arrange 3 & Act 2: Player finally gets their 4th invention of type A, completing their 2nd pair.
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(4);
        when(mockTribu.getFoodPoints()).thenReturn(20);

        EffectOutcome finalOutcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert 2: They only get rewarded for the 1 NEW pair formed (1 pair * 3 food = 3 food).
        verify(mockTribu, times(1)).addFoodPoints(FOOD_REWARD_PER_PAIR);
        assertFalse(finalOutcome.isEmpty());
        assertEquals(FOOD_REWARD_PER_PAIR, finalOutcome.resourceDeltas().get(0).delta());
    }
}