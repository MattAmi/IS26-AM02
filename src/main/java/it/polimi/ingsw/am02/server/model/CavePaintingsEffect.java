package it.polimi.ingsw.am02.server.model;


import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;

import java.util.ArrayList;
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
    public EffectOutcome applyEffect(List<Player> players) {
        List<ResourceDelta> deltas = new ArrayList<>();

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            String nickname = player.getNickname();

            int numOfArtists = tribu.getCharacterCount(CharacterType.ARTIST);

            if (numOfArtists >= minArtists) {
                int bonus = bonusPerArtist * numOfArtists;
                tribu.addPrestigePoints(bonus);
                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), bonus));
            } else {
                tribu.addPrestigePoints(-ppMalusIfFailed);
                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), -ppMalusIfFailed));
            }
        }
        return new EffectOutcome(deltas);
    }

}