package it.polimi.ingsw.am02.server.model.effect;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;

import java.util.List;

/**
 * Character effect for the Builder archetype.
 * Grants a discount on building purchases and awards prestige points credited to Builders.
 */
public class BuilderEffect implements CharacterEffect {

    private final int buildingDiscount;
    private final int prestigePoints;

    /**
     * @param buildingDiscount food points discounted from each future building purchase
     * @param prestigePoints   prestige points credited to the Builder category
     */
    public BuilderEffect(int buildingDiscount, int prestigePoints) {
        this.buildingDiscount = buildingDiscount;
        this.prestigePoints = prestigePoints;
    }

    /**
     * Applies the Builder bonus to the player's tribe:
     * increments the building discount and the Builder prestige-point counter.
     *
     * @param player the player who drew this character
     * @return an {@link EffectOutcome} with the resource deltas for building discount and PP
     */
    @Override
    public EffectOutcome applyEffect(Player player) {

        Tribu tribu = player.getTribu();
        tribu.addBuildingDiscount(buildingDiscount);
        tribu.addPPBuilders(prestigePoints);

        String nickname = player.getNickname();

        return new EffectOutcome(List.of(
                new ResourceDelta(nickname, ResourceType.BUILDING_DISCOUNT, tribu.getBuildingDiscount(), buildingDiscount),
                new ResourceDelta(nickname, ResourceType.PP_BUILDERS, tribu.getPPBuilders(), prestigePoints)
        ));
    }

}
