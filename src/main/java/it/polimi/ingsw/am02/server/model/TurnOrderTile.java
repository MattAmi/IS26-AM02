package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.TurnOrderSlotInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurnOrderTile {

    private final int numPlayers;
    private final Player[] playerPositions;
    private final int[] foodBonuses;
    private final int[] prestigePointsMalus;

    public TurnOrderTile(int numPlayers) {

        TurnOrderTile templateTurnOrderTile = GameRegistry.getInstance().getTurnOrderTile(numPlayers);
        this.numPlayers = numPlayers;
        this.playerPositions = new Player[numPlayers];
        this.foodBonuses = templateTurnOrderTile.foodBonuses.clone();
        this.prestigePointsMalus = templateTurnOrderTile.prestigePointsMalus.clone();
    }

    public TurnOrderTile(int numPlayers, List<Integer> foodBonuses, List<Integer> prestigePointsMalus) {

        this.numPlayers = numPlayers;
        this.playerPositions = new Player[numPlayers];
        this.foodBonuses = foodBonuses.stream().mapToInt(Integer::intValue).toArray();
        this.prestigePointsMalus = prestigePointsMalus.stream().mapToInt(Integer::intValue).toArray();
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public int registerPlayer(Player player) {

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == null) {
                playerPositions[i] = player;
                return i;
            }
        }
        return 0;
    }

    public void removePlayer(Player player) {

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == player) {
                playerPositions[i] = null;
                return;
            }
        }
    }

    public boolean isEmpty() {

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] != null) {
                return false;
            }
        }
        return true;
    }

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

    public int getPlayerCount() {

        int playerCount = 0;
        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] != null) {
                playerCount++;
            }
        }

        return playerCount;
    }

    public List<Player> getOrderedPlayers() {

        List<Player> orderedPlayers = new ArrayList<>();

        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] != null) {
                orderedPlayers.add(playerPositions[i]);
            }
        }
        return orderedPlayers;
    }

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

    public List<TurnOrderSlotInfo> toSlotSnapshot() {
        List<TurnOrderSlotInfo> slots = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            String occupant = (playerPositions[i] != null) ? playerPositions[i].getNickname() : null;
            slots.add(new TurnOrderSlotInfo(occupant, foodBonuses[i], prestigePointsMalus[i]));
        }
        return Collections.unmodifiableList(slots);
    }
}