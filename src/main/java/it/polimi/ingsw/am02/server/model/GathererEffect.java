package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;

import java.util.List;

/**
 * Character effect for the Gatherer archetype.
 * Grants a permanent food discount applied during Sustenance event resolution.
 */
public class GathererEffect implements CharacterEffect {

    private final int foodDiscount;

    /**
     * @param foodDiscount the number of food points discounted during each Sustenance event
     */
    public GathererEffect(int foodDiscount) {
        this.foodDiscount = foodDiscount;
    }

    /**
     * Adds the food discount to the player's tribe.
     *
     * @param player the player who drew this character
     * @return an {@link EffectOutcome} with the food-discount delta
     */
    @Override
    public EffectOutcome applyEffect(Player player) {

        Tribu tribu = player.getTribu();
        tribu.addFoodDiscount(foodDiscount);

        String nickname = player.getNickname();

        return new EffectOutcome(List.of(
                new ResourceDelta(
                        nickname,
                        ResourceType.FOOD_DISCOUNT,
                        tribu.getFoodDiscount(),
                        foodDiscount)
        ));
    }

}
