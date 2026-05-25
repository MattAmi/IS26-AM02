package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link TurnOrderFoodBonusEffect} class.
 * Ensures the effect delegates the food injection to the Game class and correctly
 * calculates the difference (snapshotting) to return an accurate ResourceDelta.
 */
class TurnOrderFoodBonusEffectTest {

    private Player mockPlayer;
    private Game mockGame;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private TurnOrderFoodBonusEffect effect;

    // Strict test constants
    private static final String PLAYER_NICKNAME = "ZetaPlayer";
    private static final PhaseType TARGET_PHASE = PhaseType.END_PLAYER_TURN;

    @BeforeEach
    void setUp() {
        // Arrange
        mockPlayer = Mockito.mock(Player.class);
        mockGame = Mockito.mock(Game.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        when(mockPlayer.getTribu()).thenReturn(mockTribu);
        when(mockPlayer.getNickname()).thenReturn(PLAYER_NICKNAME);

        effect = new TurnOrderFoodBonusEffect(mockPlayer, mockGame);
    }

    @Test
    void accept_validVisitor_callsVisitPhaseObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    // =========================================================================================
    // ON PHASE CHANGE TESTS
    // =========================================================================================

    /**
     * Tests the "snapshot" logic: the effect must read food before, trigger the game,
     * read food after, and accurately report the difference if positive.
     */
    @Test
    void onPhaseChange_endPlayerTurnWithFoodGained_triggersGameAndReturnsDelta() {
        // Arrange
        int initialFood = 10;
        int finalFood = 13;
        int expectedFoodGained = finalFood - initialFood;

        // The first time getFoodPoints() is called it returns 10, the second time 13
        when(mockTribu.getFoodPoints()).thenReturn(initialFood).thenReturn(finalFood);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(TARGET_PHASE);

        // Assert: Interactions
        verify(mockGame, times(1)).triggerTurnOrderExtraFood(mockTribu);
        verify(mockTribu, times(2)).getFoodPoints(); // Ensure both snapshots were taken

        // Assert: Outcome state
        assertNotNull(outcome);
        assertFalse(outcome.isEmpty());
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(ResourceType.FOOD, outcome.resourceDeltas().get(0).resource());
        assertEquals(expectedFoodGained, outcome.resourceDeltas().get(0).delta());
    }

    /**
     * Tests early exit when the Game class decides the player doesn't deserve any food
     * (the difference between snapshots is 0).
     */
    @Test
    void onPhaseChange_endPlayerTurnWithNoFoodGained_triggersGameAndReturnsEmpty() {
        // Arrange
        int initialFood = 10;
        int finalFood = 10; // No change

        when(mockTribu.getFoodPoints()).thenReturn(initialFood).thenReturn(finalFood);

        // Act
        EffectOutcome outcome = effect.onPhaseChange(TARGET_PHASE);

        // Assert
        verify(mockGame, times(1)).triggerTurnOrderExtraFood(mockTribu);
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty(), "Outcome must be empty if food gained is 0");
    }

    /**
     * Tests that the effect ignores all other phases.
     */
    @ParameterizedTest
    @EnumSource(value = PhaseType.class, mode = EnumSource.Mode.EXCLUDE, names = {"END_PLAYER_TURN"})
    @NullSource
    void onPhaseChange_nonEndPlayerTurnOrNullPhase_doesNothingAndReturnsEmpty(PhaseType phase) {
        // Act
        EffectOutcome outcome = effect.onPhaseChange(phase);

        // Assert
        verify(mockPlayer, never()).getTribu();
        verify(mockGame, never()).triggerTurnOrderExtraFood(any());
        assertNotNull(outcome);
        assertTrue(outcome.isEmpty());
    }
}