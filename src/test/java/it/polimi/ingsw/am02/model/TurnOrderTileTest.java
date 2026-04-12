package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;


class TurnOrderTileTest {

    private TurnOrderTile tile;
    private Player matteo;
    private Player raed;

    @BeforeEach
    void setUp() {
        tile = new TurnOrderTile(2, List.of(1, -1), List.of(0, -2));
        matteo = new Player("Matteo", Totem.WHITE);
        raed = new Player("Raed", Totem.BLUE);
    }

    // --- Happy paths ---

    @Test
    @DisplayName("registerPlayer should place player in first free slot")
    void shouldRegisterPlayerInFirstFreeSlot() {
        tile.registerPlayer(matteo);

        assertEquals(1, tile.getPlayerCount());
        assertFalse(tile.isEmpty());
    }

    @Test
    @DisplayName("isEmpty should return true when no players are registered")
    void shouldBeEmptyInitially() {
        assertTrue(tile.isEmpty());
        assertEquals(0, tile.getPlayerCount());
    }

    @Test
    @DisplayName("isEmpty should return false after one player registered, true after all removed")
    void shouldTrackEmptyStateCorrectly() {
        tile.registerPlayer(matteo);
        assertFalse(tile.isEmpty());

        tile.removePlayer(matteo);
        assertTrue(tile.isEmpty());
    }

    @Test
    @DisplayName("getPlayerCount should reflect registrations and removals")
    void shouldCountPlayersCorrectly() {
        assertEquals(0, tile.getPlayerCount());

        tile.registerPlayer(matteo);
        assertEquals(1, tile.getPlayerCount());

        tile.registerPlayer(raed);
        assertEquals(2, tile.getPlayerCount());

        tile.removePlayer(matteo);
        assertEquals(1, tile.getPlayerCount());
    }

    @Test
    @DisplayName("getOrderedPlayers should return players in registration order when both slots are occupied")
    void getOrderedPlayersBothSlotsOccupiedReturnsPlayersInOrder() {
        tile.registerPlayer(matteo);
        tile.registerPlayer(raed);

        List<Player> result = tile.getOrderedPlayers();

        assertEquals(2, result.size());
        assertEquals(matteo, result.get(0));
        assertEquals(raed, result.get(1));
    }

    @Test
    @DisplayName("getOrderedPlayers should return only the registered player when a single slot is occupied")
    void getOrderedPlayersOneSlotOccupiedReturnsSinglePlayer() {
        tile.registerPlayer(matteo);

        List<Player> result = tile.getOrderedPlayers();

        assertEquals(1, result.size());
        assertEquals(matteo, result.getFirst());
    }

    @Test
    @DisplayName("getOrderedPlayers should return an empty list when no player has been registered")
    void getOrderedPlayersNoPlayersRegisteredReturnsEmptyList() {
        List<Player> result = tile.getOrderedPlayers();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getFoodForPlayer should return the correct food bonus for the player in the first slot (positive bonus)")
    void getFoodForPlayerFirstSlotReturnsPositiveBonus() {
        tile.registerPlayer(matteo);

        int food = tile.getFoodForPlayer(matteo);

        assertEquals(1, food);
    }

    @Test
    @DisplayName("getFoodForPlayer should return the correct food bonus for the player in the second slot (negative bonus)")
    void getFoodForPlayerSecondSlotReturnsNegativeValue() {
        tile.registerPlayer(matteo);
        tile.registerPlayer(raed);

        int food = tile.getFoodForPlayer(raed);

        assertEquals(-1, food);
    }

    @Test
    @DisplayName("getFoodForPlayer should return 0 when the player is not registered in any slot")
    void getFoodForPlayerPlayerNotRegisteredReturnsZero() {
        int food = tile.getFoodForPlayer(matteo);

        assertEquals(0, food);
    }

    @Test
    @DisplayName("applyRewards should add food to a player occupying the first free slot (positive food bonus)")
    void applyRewardsFirstSlotPositiveBonusAddsFoodToPlayer() {
        tile.registerPlayer(matteo);
        int foodBefore = matteo.getTribu().getFoodPoints();

        tile.applyRewards(matteo);

        assertEquals(foodBefore + 1, matteo.getTribu().getFoodPoints());
    }

    @Test
    @DisplayName("applyRewards should deduct food from the player in the second slot when the player has enough food to pay the malus")
    void applyRewardsSecondSlotNegativeBonusPlayerHasFoodDeductsFood() {
        tile.registerPlayer(matteo);
        tile.registerPlayer(raed);
        raed.getTribu().addFoodPoints(3);

        int foodBefore = raed.getTribu().getFoodPoints();
        tile.applyRewards(raed);

        assertEquals(foodBefore - 1, raed.getTribu().getFoodPoints());
        // no PP deducted
        assertEquals(0, raed.getTribu().getPrestigePoints());
    }

    @Test
    @DisplayName("applyRewards should deduct prestige points when a player in the\n" +
            "     * second slot does not have enough food to pay the malus")
    void applyRewardsSecondSlotNegativeBonusPlayerLacksFoodDeductsPP() {
        tile.registerPlayer(matteo);
        tile.registerPlayer(raed);
        // raed has 0 food: he can't pay the -1 food malus

        tile.applyRewards(raed);

        assertEquals(0, raed.getTribu().getFoodPoints());
        assertEquals(-2, raed.getTribu().getPrestigePoints()); // prestigePointsMalus[1] = -2
    }

}