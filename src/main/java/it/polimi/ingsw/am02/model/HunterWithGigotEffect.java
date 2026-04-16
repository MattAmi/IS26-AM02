package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.enumerations.CharacterType;

public class HunterWithGigotEffect implements CharacterEffect {

    public HunterWithGigotEffect() {}

    //Hunter Effect: it adds as much food as the number of hunters in the tribu
    @Override
    public void applyEffect(Tribu tribu) {
        int numOfHunters = tribu.getCharacterCount(CharacterType.HUNTER);
        tribu.addFoodPoints(numOfHunters);
    }

}
