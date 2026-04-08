package it.polimi.ingsw.am02.model;

public class NoEffect implements CharacterEffect {

    //intentionally left empty, it serves in the cases where the character doesn't have a direct impact once drawn
    //for example when i draw a hunter without a gigot, it doesn't have any immediate effect
    @Override
    public void applyEffect(Tribu tribu) {}
}
