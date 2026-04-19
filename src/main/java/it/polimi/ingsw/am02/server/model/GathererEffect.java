package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;

import java.util.List;

public class GathererEffect implements CharacterEffect {

    private final int foodDiscount;

    //Costruttore
    public GathererEffect(int foodDiscount) {
        this.foodDiscount = foodDiscount;
    }

    //Gatherer's Effect: adds some foodDiscount to the tribu
    @Override
    public EffectOutcome applyEffect(Player player) {

        Tribu tribu = player.getTribu();
        tribu.addFoodDiscount(foodDiscount);

        return new EffectOutcome(List.of(
                new ResourceDelta(
                        player.getNickname(),
                        ResourceType.FOOD_DISCOUNT,
                        tribu.getFoodDiscount(),
                        foodDiscount)
        ));
    }

}
