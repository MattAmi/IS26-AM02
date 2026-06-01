package it.polimi.ingsw.am02.server.model.effect;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;

import java.util.List;

/**
 * Character effect for the Shaman archetype.
 * Adds shaman stars to the tribe, used in Shamanic Ritual event resolution.
 */
public class ShamanEffect implements CharacterEffect {

    private final int shamanStars;

    /**
     * @param shamanStars the number of shaman stars to add when this character is drawn
     */
    public ShamanEffect(int shamanStars) {
        this.shamanStars = shamanStars;
    }

    /**
     * Adds shaman stars to the player's tribe.
     *
     * @param player the player who drew this character
     * @return an {@link EffectOutcome} with the shaman-stars delta
     */
    @Override
    public EffectOutcome applyEffect(Player player) {

        Tribu tribu = player.getTribu();
        tribu.addShamanStars(shamanStars);

        String nickname = player.getNickname();

        return new EffectOutcome(List.of(
                new ResourceDelta(
                        nickname,
                        ResourceType.SHAMAN_STARS,
                        tribu.getShamanStars(),
                        shamanStars)
        ));
    }

}
