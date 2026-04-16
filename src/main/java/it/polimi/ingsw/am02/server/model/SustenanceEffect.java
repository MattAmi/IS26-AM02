package it.polimi.ingsw.am02.server.model;

import java.util.List;

public class SustenanceEffect implements EventEffect {

    //Attributi
    private final int penaltyPerUnfed;

    public SustenanceEffect(int penaltyPerUnfed) {
        this.penaltyPerUnfed = penaltyPerUnfed;
    }

    //Metodi
    @Override
    public void applyEffect(List<Player> players) {
        for (Player player : players) {
            Tribu tribu = player.getTribu();

            //verify the amount of food to be paid, applying both the gatherer and building discounts (calculated in tribu)
            int netCost = tribu.calculateSustenanceCost();
            //check how much they can actually pay with food (how much food do they have?)
            int canBePayed = tribu.getFoodPoints();
            //pay the actual amount due in food (it will be the minimum of what I owe and what I have)
            int effectivelyPayed = Math.min(netCost, canBePayed);
            tribu.addFoodPoints(-effectivelyPayed);
            //if the net cost is higher than the actual food paid, I have to pay the remainder in aura points
            int stillToBePayed = netCost - effectivelyPayed;

            if (stillToBePayed > 0) {
                int toPayWithPP = stillToBePayed * this.penaltyPerUnfed;
                tribu.addPrestigePoints(-toPayWithPP); //pay with pp
            }
        }
    }

}
