package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;

import java.util.List;

public class ShamanEffect implements CharacterEffect {

    private final int shamanStars;

    //Costruttore
    public ShamanEffect(int shamanStars) {
        this.shamanStars = shamanStars;
    }

    //Shaman's Effect: add stars to the tribu
    @Override
    public EffectOutcome applyEffect(Player player) {

        Tribu tribu = player.getTribu();
        tribu.addShamanStars(shamanStars);

        return new EffectOutcome(List.of(
                new ResourceDelta(
                        player.getNickname(),
                        ResourceType.SHAMAN_STARS,
                        tribu.getShamanStars(),
                        shamanStars)
        ));
    }

}
