package it.polimi.ingsw.am02.server.model;

public class BuilderEffect implements CharacterEffect {

    private final int buildingDiscount;
    private final int prestigePoints;

    //Costruttore
    public BuilderEffect(int buildingDiscount, int prestigePoints) {
        this.buildingDiscount = buildingDiscount;
        this.prestigePoints = prestigePoints;
    }

    //Builder's Effect: adds a food discount when the player wants to buy a building
    @Override
    public void applyEffect(Tribu tribu) {
        tribu.addBuildingDiscount(buildingDiscount);
        tribu.addPPBuilders(prestigePoints);
    }

}
