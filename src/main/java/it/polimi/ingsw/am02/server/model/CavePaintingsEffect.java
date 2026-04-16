package it.polimi.ingsw.am02.server.model;


import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;

import java.util.List;

public class CavePaintingsEffect implements EventEffect{

    //Attributi
    final int minArtists; //minimum number to profit from the event
    final int ppMalusIfFailed;
    final int bonusPerArtist;

    //Costruttore
    public CavePaintingsEffect(int minArtists, int malusIfFailed, int bonusPerArtist) {
        this.minArtists = minArtists;
        this.ppMalusIfFailed = malusIfFailed;
        this.bonusPerArtist = bonusPerArtist;
    }

    //Metodi
    @Override
    public void applyEffect(List<Player> players) {
        for (Player player : players) {
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