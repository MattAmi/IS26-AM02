package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.TurnOrderSlotInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The turn-order tile: a fixed-size array of player positions that determines
 * the order in which players place their totems in the next round.
 *
 * <p>Each position carries a food bonus (positive) or food penalty (negative).
 * If a player cannot pay a food penalty, they lose prestige points instead.
 */
public class TurnOrderTile {

    private final int numPlayers;
    private final Player[] playerPositions;
    private final int[] foodBonuses;
    private final int[] prestigePointsMalus;

    /**
     * Creates a turn-order tile by cloning the template registered in {@link GameRegistry}
     * for the given player count.
     *
     * @param numPlayers the number of players (must match a registered template)
     */
    public TurnOrderTile(int numPlayers) {

        TurnOrderTile templateTurnOrderTile = GameRegistry.getInstance().getTurnOrderTile(numPlayers);
        this.numPlayers = numPlayers;
        this.playerPositions = new Player[numPlayers];
        this.foodBonuses = templateTurnOrderTile.foodBonuses.clone();
        this.prestigePointsMalus = templateTurnOrderTile.prestigePointsMalus.clone();
    }

    /**
     * Creates a turn-order tile directly from explicit bonus and malus lists.
     * Used by {@link TurnOrderTilesFactory} during JSON deserialisation.
     *
     * @param numPlayers          the number of players
     * @param foodBonuses         food reward (positive) or penalty (negative) per position
     * @param prestigePointsMalus prestige-point penalty per position (stored as negative values)
     */
    public TurnOrderTile(int numPlayers, List<Integer> foodBonuses, List<Integer> prestigePointsMalus) {

        this.numPlayers = numPlayers;
        this.playerPositions = new Player[numPlayers];
        this.foodBonuses = foodBonuses.stream().mapToInt(Integer::intValue).toArray();
        this.prestigePointsMalus = prestigePointsMalus.stream().mapToInt(Integer::intValue).toArray();
    }

    /** @return the number of player slots on this tile */
    public int getNumPlayers() {
        return numPlayers;
    }

    /**
     * Places the given player in the first available (null) slot.
     *
     * @param player the player to register
     * @return the slot index assigned to the player (0-based)
     */
    public int registerPlayer(Player player) {

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == null) {
                playerPositions[i] = player;
                return i;
            }
        }
        return 0;
    }

    /**
     * Removes the given player from their current slot, freeing it.
     *
     * @param player the player to remove
     */
    public void removePlayer(Player player) {

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == player) {
                playerPositions[i] = null;
                return;
            }
        }
    }

    /** @return {@code true} if all player slots are empty */
    public boolean isEmpty() {

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies end-of-turn food bonuses and penalties for the given player
     * based on their position on the tile.
     * If a food penalty cannot be fully paid in food, prestige points are deducted instead.
     *
     * @param player the player whose end-of-turn rewards are being resolved
     * @return a {@link TurnOrderRewardResult} summarising what was gained and lost
     */
    public TurnOrderRewardResult applyRewards(Player player) {

        int playerIndex = -1;
        for (int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == player) {
                playerIndex = i;
                break;
            }
        }
        if (playerIndex == -1) {
            return TurnOrderRewardResult.empty();
        }

        int foodBonus = foodBonuses[playerIndex];

        if (foodBonus > 0) {
            player.getTribu().addFoodPoints(foodBonus);
            return new TurnOrderRewardResult(foodBonus, 0, 0);
        }

        if (foodBonus == 0) {
            return TurnOrderRewardResult.empty();
        }

        int foodToPay = -foodBonus;
        int currentFood = player.getTribu().getFoodPoints();

        if (currentFood >= foodToPay) {
            player.getTribu().addFoodPoints(foodBonus);
            return new TurnOrderRewardResult(0, foodToPay, 0);
        } else {
            int ppPenalty = -prestigePointsMalus[playerIndex];  // store as positive
            player.getTribu().addPrestigePoints(prestigePointsMalus[playerIndex]);
            return new TurnOrderRewardResult(0, 0, ppPenalty);
        }
    }

    /** @return the number of players currently registered on this tile */
    public int getPlayerCount() {

        int playerCount = 0;
        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] != null) {
                playerCount++;
            }
        }

        return playerCount;
    }

    /**
     * @return an ordered list of players currently on the tile, from slot 0 to the last slot
     *         (null slots are skipped)
     */
    public List<Player> getOrderedPlayers() {

        List<Player> orderedPlayers = new ArrayList<>();

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] != null) {
                orderedPlayers.add(playerPositions[i]);
            }
        }
        return orderedPlayers;
    }

    /**
     * Returns the food value associated with the given player's current position.
     *
     * @param player the player to look up
     * @return the food bonus (positive) or penalty (negative) for the player's slot,
     *         or {@code 0} if the player is not on this tile
     */
    public int getFoodForPlayer(Player player) {

        for (int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == player) {
                return foodBonuses[i];
            }
        }
        return 0;
    }


    public record TurnOrderRewardResult(int foodGained, int foodPenalty, int ppPenalty) {

        public static TurnOrderRewardResult empty() {
            return new TurnOrderRewardResult(0, 0, 0);
        }
    }

    /**
     * Builds an immutable snapshot of all slots for client transmission.
     *
     * @return an unmodifiable list of {@link TurnOrderSlotInfo} DTOs, one per slot
     */
    public List<TurnOrderSlotInfo> toSlotSnapshot() {
        List<TurnOrderSlotInfo> slots = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String occupant = (playerPositions[i] != null) ? playerPositions[i].getNickname() : null;
            slots.add(new TurnOrderSlotInfo(occupant, foodBonuses[i], prestigePointsMalus[i]));
        }
        return Collections.unmodifiableList(slots);
    }
}