package it.polimi.ingsw.am02.model;

import java.util.ArrayList;
import java.util.List;

public class TurnOrderTile {

    private final int numPlayers;
    private final Player[] playerPositions;
    private final int[] foodBonuses;
    private final int[] prestigePointsMalus;

    public TurnOrderTile(int numPlayers) {
        TurnOrderTile templateTurnOrderTile = GameRegistry.getTurnOrderTile(numPlayers);
        this.numPlayers = numPlayers;
        this.playerPositions = new Player[numPlayers];
        this.foodBonuses = templateTurnOrderTile.foodBonuses.clone();
        this.prestigePointsMalus = templateTurnOrderTile.prestigePointsMalus.clone();
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public void registerPlayer(Player player) {
        for(int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == null) {
                playerPositions[i] = player;
                return;
            }
        }
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

    public void applyRewards(Player player) {
        int playerIndex = -1;
        for (int i = 0; i < playerPositions.length; i++) {
            if (playerPositions[i] == player) {
                playerIndex = i;
                break;
            }
        }
        if (playerIndex == -1) return;

        int foodBonus = foodBonuses[playerIndex];

        if (foodBonus > 0) {
            player.getTribu().addFoodPoints(foodBonus);
        } else if (foodBonus < 0) {
            int currentTribuFood = player.getTribu().getFoodPoints();
            int foodToPay = -foodBonus;

            if (currentTribuFood >= foodToPay) {
                player.getTribu().addFoodPoints(foodBonus);
            } else {
                player.getTribu().addFoodPoints(-currentTribuFood);
                int malusPrestigePoints = prestigePointsMalus[playerIndex];
                player.getTribu().addPrestigePoints(malusPrestigePoints);
            }
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
}