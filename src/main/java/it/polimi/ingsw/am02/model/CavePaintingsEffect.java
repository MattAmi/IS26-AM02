package it.polimi.ingsw.am02.model;


import it.polimi.ingsw.am02.model.Enumerations.CharacterType;

public class CavePaintingsEffect implements EventEffect{

    //Attributi
    int minArtists; //minimum number to profit from the event
    int ppMalusIfFailed;
    int bonusPerArtist;

    //Costruttore
    public CavePaintingsEffect(int minArtists, int bonusPerArtist) {
        this.minArtists = minArtists;
        this.bonusPerArtist = bonusPerArtist;
    }


    //Metodi
    @Override
    public void applyEffect(Game game) {

        for (Player player : game.getPlayers()) {
            Tribu tribu = player.getTribu();
            int numOfArtists = tribu.getCharacterCount(CharacterType.ARTIST);
            if(numOfArtists >= minArtists){
                //win points
                tribu.addPrestigePoints(bonusPerArtist * numOfArtists);
            }else{ //case: if(numOfArtists < minArtists)
                //take off points
                tribu.addPrestigePoints(-ppMalusIfFailed);
            }
        }
    }

}