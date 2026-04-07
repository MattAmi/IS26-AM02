package it.polimi.ingsw.am02.model;

public class GathererEffect implements CharacterEffect {

    private final int foodDiscount;

    //Costruttore
    public GathererEffect(int foodDiscount) {
        this.foodDiscount = foodDiscount;
    }

    //Gatherer's Effect: adds some foodDiscount to the tribu
    @Override
    public void applyEffect(Tribu tribu) {
        tribu.addFoodDiscount(foodDiscount);
    }
}
