package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;

import java.util.List;

public class BuilderEffect implements CharacterEffect {

    private final int buildingDiscount;
    private final int prestigePoints;

    //Costruttore
    public BuilderEffect(int buildingDiscount, int prestigePoints) {
        this.buildingDiscount = buildingDiscount;
        this.prestigePoints = prestigePoints;
    }

    //Builder's Effect: adds a food discount when the player wants to buy a building
    @Override
    public EffectOutcome applyEffect(Player player) {

        Tribu tribu = player.getTribu();
        tribu.addBuildingDiscount(buildingDiscount);
        tribu.addPPBuilders(prestigePoints);

        return new EffectOutcome(List.of(
                new ResourceDelta(player.getNickname(), ResourceType.BUILDING_DISCOUNT, tribu.getBuildingDiscount(), buildingDiscount),
                new ResourceDelta(player.getNickname(), ResourceType.PP_BUILDERS, tribu.getPPBuilders(), prestigePoints)
        ));
    }

}
