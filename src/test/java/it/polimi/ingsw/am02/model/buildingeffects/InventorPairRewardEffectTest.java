package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.model.enumerations.InventionType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class InventorPairRewardEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;

    // Strict test constants (No Magic Numbers)
    private final int FOOD_REWARD_PER_PAIR = 3;
    private final CharacterType TRIGGERING_CHARACTER = CharacterType.INVENTOR;
    private final CharacterType NON_TRIGGERING_CHARACTER = CharacterType.HUNTER;

    // We safely use the first two values of the Enum to avoid hardcoding specific names that might change
    private final InventionType INVENTION_A = InventionType.values()[0];
    private final InventionType INVENTION_B = InventionType.values()[1];

    @BeforeEach
    void setUp() {
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Default state: The player starts with 0 of every invention
        when(mockTribu.getInventionTypeCount(any(InventionType.class))).thenReturn(0);
    }

    @Test
    void testAcceptCallsVisitTribuObserver() {
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockTribu, FOOD_REWARD_PER_PAIR);

        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitTribuObserver(effect);
    }

    @Test
    void testOnCharacterInsertion_WithWrongCharacter_ShouldDoNothing() {
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockTribu, FOOD_REWARD_PER_PAIR);

        Mockito.clearInvocations(mockTribu);

        // Act: Insert a character that is NOT an inventor
        effect.onCharacterInsertion(NON_TRIGGERING_CHARACTER);

        // Assert: The effect must immediately exit without checking the Tribu
        verify(mockTribu, never()).getInventionTypeCount(any());
        verify(mockTribu, never()).addFoodPoints(anyInt());
    }

    @Test
    void testOnCharacterInsertion_WithInventorButNoNewPair_ShouldNotReward() {
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockTribu, FOOD_REWARD_PER_PAIR);

        // Fast-forward state: Player gets 1 invention of type A. (1 / 2 = 0 pairs)
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(1);

        // Act: Trigger the insertion
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: Because 1 invention doesn't make a pair, no food is rewarded
        verify(mockTribu, never()).addFoodPoints(anyInt());
    }

    @Test
    void testOnCharacterInsertion_WhenPairCompleted_ShouldAddFood() {
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockTribu, FOOD_REWARD_PER_PAIR);

        // Fast-forward state: Player gets their 2nd invention of type A, completing exactly 1 pair.
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(2);

        // Act: Trigger the insertion
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: 1 pair was formed, so exactly 3 food should be added
        verify(mockTribu, times(1)).addFoodPoints(FOOD_REWARD_PER_PAIR);
    }

    @Test
    void testOnCharacterInsertion_WhenMultiplePairsCompletedAtOnce_ShouldAddMultipliedFood() {
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockTribu, FOOD_REWARD_PER_PAIR);

        // Fast-forward state: A massive combo happens where they suddenly have 2 of A and 2 of B (2 new pairs)
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(2);
        when(mockTribu.getInventionTypeCount(INVENTION_B)).thenReturn(2);

        int expectedFood = FOOD_REWARD_PER_PAIR * 2; // 3 * 2 = 6

        // Act
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: Evaluates the "newPairsFormed" multiplier logic in your code
        verify(mockTribu, times(1)).addFoodPoints(expectedFood);
    }

    @Test
    void testRetroactiveRewardPrevention() {
        // Arrange: Player ALREADY has 3 inventions of type A (which means 1 full pair exists)
        // BEFORE the building is constructed.
        int existingInventions = 3;
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(existingInventions);

        // Act 1: The effect is created. It MUST memorize the existing 1 pair.
        InventorPairRewardEffect effect = new InventorPairRewardEffect(mockTribu, FOOD_REWARD_PER_PAIR);

        // Act 2: Player inserts another Inventor, but it doesn't form a pair (still 3 inventions).
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert 1: No food rewarded retroactively, and no food for the 3rd odd invention.
        verify(mockTribu, never()).addFoodPoints(anyInt());

        // Act 3: Player finally gets their 4th invention of type A, completing their 2nd pair.
        when(mockTribu.getInventionTypeCount(INVENTION_A)).thenReturn(4);
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert 2: They only get rewarded for the NEW pair formed (1 pair * 3 food = 3 food).
        verify(mockTribu, times(1)).addFoodPoints(FOOD_REWARD_PER_PAIR);
    }
}