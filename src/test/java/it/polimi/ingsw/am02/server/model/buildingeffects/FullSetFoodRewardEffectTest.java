package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link FullSetFoodRewardEffect} class.
 * Ensures the effect correctly tracks the number of completed character sets
 * and exclusively rewards the player when a new full set is formed,
 * preventing retroactive rewards for sets completed prior to acquiring the effect.
 */
class FullSetFoodRewardEffectTest {

    private Player mockPlayer;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;

    // Strict test constants
    private static final int FOOD_BONUS = 3;
    private static final String PLAYER_NICKNAME = "EchoPlayer";
    private static final CharacterType TRIGGERING_CHARACTER = CharacterType.SHAMAN;
    private static final CharacterType MISSING_CHARACTER = CharacterType.GATHERER;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize mocks
        mockPlayer = Mockito.mock(Player.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // Standard mock links
        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);
    }

    /**
     * Tests that the visitor pattern is correctly implemented.
     */
    @Test
    void accept_validVisitor_callsVisitTribuObserver() {
        // Arrange: default setup (0 characters)
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(0);
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockPlayer, FOOD_BONUS);

        // Act
        effect.accept(mockVisitor);

        // Assert
        verify(mockVisitor, times(1)).visitTribuObserver(effect);
    }

    // =========================================================================================
    // ON CHARACTER INSERTION TESTS
    // =========================================================================================

    /**
     * Tests that the player receives the food reward and a populated DTO
     * when the inserted character completes a brand new full set.
     */
    @Test
    void onCharacterInsertion_setCompleted_addsFoodAndReturnsDelta() {
        // Arrange: The player starts with 0 sets when the effect is created
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(0);
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockPlayer, FOOD_BONUS);

        // Fast-forward game state: The player just added the final character they needed.
        // Now, the Tribu reports 1 of EVERY character.
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(1);
        when(mockTribu.getFoodPoints()).thenReturn(10); // Simulated food total after addition

        // Act: The observer notification fires
        EffectOutcome outcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: Verify state changes and interactions
        verify(mockTribu, times(1)).addFoodPoints(FOOD_BONUS);

        // Assert: Verify the outcome payload
        assertNotNull(outcome, "EffectOutcome should not be null");
        assertFalse(outcome.isEmpty(), "EffectOutcome should contain a ResourceDelta");
        assertEquals(1, outcome.resourceDeltas().size(), "There should be exactly one delta");
        assertEquals(ResourceType.FOOD, outcome.resourceDeltas().get(0).resource(), "Delta should be for FOOD");
        assertEquals(FOOD_BONUS, outcome.resourceDeltas().get(0).delta(), "Delta amount should match the food bonus");
    }

    /**
     * Tests that the player receives no reward if the inserted character does not
     * complete a full set (i.e., a bottleneck character is still missing).
     */
    @Test
    void onCharacterInsertion_setNotCompleted_doesNothingAndReturnsEmpty() {
        // Arrange: The player starts with 0 sets
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(0);
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockPlayer, FOOD_BONUS);

        // Fast-forward game state: The player has 1 of almost everything,
        // BUT they still have 0 of the MISSING_CHARACTER (Bottleneck = 0 sets).
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(1);
        when(mockTribu.getCharacterCount(MISSING_CHARACTER)).thenReturn(0);

        // Act: Player inserted a Shaman, but still lacks a Gatherer
        EffectOutcome outcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert: No food should be added
        verify(mockTribu, never()).addFoodPoints(anyInt());

        assertNotNull(outcome, "EffectOutcome should not be null");
        assertTrue(outcome.isEmpty(), "EffectOutcome should be empty when no set is completed");
    }

    /**
     * Tests that the effect correctly memorizes the sets completed BEFORE it was acquired,
     * preventing the player from receiving retroactive rewards for past achievements.
     */
    @Test
    void onCharacterInsertion_playerAlreadyHasSets_doesNotRewardRetroactively() {
        // Arrange 1: The player ALREADY has 2 full sets BEFORE they buy this building.
        int existingSets = 2;
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(existingSets);

        // The building is bought and the effect is created. It memorizes 2 sets.
        FullSetFoodRewardEffect effect = new FullSetFoodRewardEffect(mockPlayer, FOOD_BONUS);

        // Arrange 2 & Act 1: The player inserts a character, but it's just their 3rd Shaman.
        // They still only have 2 FULL sets (the bottleneck is still 2).
        when(mockTribu.getCharacterCount(TRIGGERING_CHARACTER)).thenReturn(3);
        EffectOutcome firstOutcome = effect.onCharacterInsertion(TRIGGERING_CHARACTER);

        // Assert 1: No food added, because 3 full sets haven't been reached yet.
        verify(mockTribu, never()).addFoodPoints(anyInt());
        assertTrue(firstOutcome.isEmpty());

        // Arrange 3 & Act 2: Now the player completes their 3rd full set
        // (they finally get their 3rd copy of the bottleneck character).
        when(mockTribu.getCharacterCount(any(CharacterType.class))).thenReturn(3);
        when(mockTribu.getFoodPoints()).thenReturn(20);

        EffectOutcome finalOutcome = effect.onCharacterInsertion(MISSING_CHARACTER);

        // Assert 2: NOW they should get the reward, exactly once.
        verify(mockTribu, times(1)).addFoodPoints(FOOD_BONUS);
        assertFalse(finalOutcome.isEmpty());
        assertEquals(FOOD_BONUS, finalOutcome.resourceDeltas().get(0).delta());
    }
}