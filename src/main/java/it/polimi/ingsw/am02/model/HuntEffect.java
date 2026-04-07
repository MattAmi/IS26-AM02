package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.CharacterType;

public class HuntEffect implements EventEffect{

    //Attributi
    int foodPerHunter;
    int ppPerHunter;

    //Costruttore
    public HuntEffect(int foodPerHunter, int ppPerHunter) {
        this.foodPerHunter = foodPerHunter;
        this.ppPerHunter = ppPerHunter;
    }


    @Override
    public void applyEffect(Game game) {

        for (Player player : game.getPlayers()) {
            Tribu tribu = player.getTribu();
            int numOfHunters = tribu.getCharacterCount(CharacterType.HUNTER);
            if(numOfHunters> 0){ //if I have at least one hunter I can get the bonuses
                tribu.addFoodPoints(foodPerHunter * numOfHunters);
                tribu.addPrestigePoints(ppPerHunter * numOfHunters);
            }
        }
    }
}
