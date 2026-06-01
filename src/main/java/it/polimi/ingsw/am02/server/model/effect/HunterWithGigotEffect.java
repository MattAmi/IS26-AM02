package it.polimi.ingsw.am02.server.model.effect;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;

import java.util.List;

/**
 * Character effect for a Hunter character carrying a gigot.
 * Immediately grants food equal to the total number of Hunters in the tribe.
 */
public class HunterWithGigotEffect implements CharacterEffect {

    public HunterWithGigotEffect() {}

    /**
     * Grants food equal to the current number of Hunters (including this one)
     * in the player's tribe.
     *
     * @param player the player who drew this character
     * @return an {@link EffectOutcome} with the food delta, or an empty outcome if
     *         there are no Hunters in the tribe
     */
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
