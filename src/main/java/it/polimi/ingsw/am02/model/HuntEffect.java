package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.enumerations.CharacterType;

import java.util.List;

public class HuntEffect implements EventEffect {

    //Attributi
    final int foodPerHunter;
    final int ppPerHunter;

    //Costruttore
    public HuntEffect(int foodPerHunter, int ppPerHunter) {
        this.foodPerHunter = foodPerHunter;
        this.ppPerHunter = ppPerHunter;
    }

    @Override
    public void applyEffect(List<Player> players) {

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            int numOfHunters = tribu.getCharacterCount(CharacterType.HUNTER);
            if(numOfHunters > 0) { //if I have at least one hunter I can get the bonuses
                tribu.addFoodPoints(foodPerHunter * numOfHunters);
                tribu.addPrestigePoints(ppPerHunter * numOfHunters);
            }
        }
    }

}
