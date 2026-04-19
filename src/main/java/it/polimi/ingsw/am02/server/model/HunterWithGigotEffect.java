package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;

import java.util.List;

public class HunterWithGigotEffect implements CharacterEffect {

    public HunterWithGigotEffect() {}

    //Hunter Effect: it adds as much food as the number of hunters in the tribu
    @Override
    public EffectOutcome applyEffect(Player player) {
        Tribu tribu = player.getTribu();

        int numOfHunters = tribu.getCharacterCount(CharacterType.HUNTER);

        if (numOfHunters <= 0) {
            return EffectOutcome.empty();
        }

        tribu.addFoodPoints(numOfHunters);

        String nickname = player.getNickname();

        ResourceDelta foodDelta = new ResourceDelta(
                nickname,
                ResourceType.FOOD,
                tribu.getFoodPoints(),
                numOfHunters);

        return new EffectOutcome(List.of(foodDelta));
    }

}
