package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.buildingeffects.FullSetFoodRewardEffect;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class FullSetFoodRewardEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;

    // Strict test constants (No Magic Numbers)
    private final int FOOD_BONUS = 3;
    private final CharacterType TRIGGERING_CHARACTER = CharacterType.SHAMAN;
    private final CharacterType MISSING_CHARACTER = CharacterType.GATHERER;

    @BeforeEach
    void setUp() {
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Default state: Player starts with absolutely 0 characters
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(0);
    }

    @Test
    void testAcceptCallsVisitTribuObserver() {
        // Instantiate with 0 initial sets
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockTribu, FOOD_BONUS);

        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitTribuObserver(effect);
    }

    @Test
    void testOnCharacterInsertion_WhenSetCompleted_ShouldAddFood() {
        // Arrange: Start with 0 sets
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockTribu, FOOD_BONUS);

        // Fast-forward game state: The player just added the final character they needed.
        // Now, if you ask the Tribu, it says "I have 1 of EVERY character".
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(1);

        // Act: The observer notification fires
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: The minimum went from 0 to 1. The food bonus MUST be applied.
        verify(mockTribu, times(1)).addFoodPoints(FOOD_BONUS);
    }

    @Test
    void testOnCharacterInsertion_WhenSetNotCompleted_ShouldDoNothing() {
        // Arrange: Start with 0 sets
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockTribu, FOOD_BONUS);

        // Fast-forward game state: The player has 1 of almost everything,
        // BUT they still have 0 of the MISSING_CHARACTER.
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(1);
        when(mockTribu.getCharacterCount(MISSING_CHARACTER)).thenReturn(0);

        // Act: The observer notification fires (player inserted a Shaman, but still lacks a Gatherer)
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: The minimum is still 0. No food should be added.
        verify(mockTribu, never()).addFoodPoints(anyInt());
    }

    @Test
    void testConstructor_WhenPlayerAlreadyHasSets_ShouldNotRewardRetroactively() {
        // Arrange: The player ALREADY has 2 full sets BEFORE they buy this building.
        int existingSets = 2;
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(existingSets);

        // Act 1: The building is bought and the effect is created.
        // It should memorize that 2 sets are already complete.
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockTribu, FOOD_BONUS);

        // Act 2: The player inserts a character, but it's just their 3rd Shaman.
        // They still only have 2 FULL sets.
        when(mockTribu.getCharacterCount(TRIGGERING_CHARACTER)).thenReturn(3);
        effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: No food should be added because they haven't reached 3 full sets yet.
        verify(mockTribu, never()).addFoodPoints(anyInt());

        // Act 3: Now the player completes their 3rd full set (they get their 3rd of everything).
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(3);
        effect.onCharacterInsertion(MISSING_CHARACTER); // Finally got the missing piece

        // Assert: NOW they should get the reward, exactly once.
        verify(mockTribu, times(1)).addFoodPoints(FOOD_BONUS);
    }
}