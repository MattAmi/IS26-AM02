package it.polimi.ingsw.am02.model;

public class ShamanEffect implements CharacterEffect {

    private final int shamanStars;

    //Costruttore
    public ShamanEffect(int shamanStars) {
        this.shamanStars = shamanStars;
    }

    //Shaman's Effect: add stars to the tribu
    @Override
    public void applyEffect(Tribu tribu) {
        tribu.addShamanStars(shamanStars);
    }

}
