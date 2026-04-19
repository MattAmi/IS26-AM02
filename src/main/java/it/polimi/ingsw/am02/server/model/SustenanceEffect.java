package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;

import java.util.ArrayList;
import java.util.List;

public class SustenanceEffect implements EventEffect {

    //Attributi
    private final int penaltyPerUnfed;

    public SustenanceEffect(int penaltyPerUnfed) {
        this.penaltyPerUnfed = penaltyPerUnfed;
    }

    //Metodi
    @Override
    public EffectOutcome applyEffect(List<Player> players) {
        List<ResourceDelta> deltas = new ArrayList<>();

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            String nickname = player.getNickname();

            int netCost = tribu.calculateSustenanceCost();
            int canBePayed = tribu.getFoodPoints();
            int effectivelyPayed = Math.min(netCost, canBePayed);

            if (effectivelyPayed > 0) {
                tribu.addFoodPoints(-effectivelyPayed);
                deltas.add(new ResourceDelta(nickname, ResourceType.FOOD, tribu.getFoodPoints(), -effectivelyPayed));
            }


            int stillToBePayed = netCost - effectivelyPayed;
            if (stillToBePayed > 0) {
                int toPayWithPP = stillToBePayed * this.penaltyPerUnfed;
                tribu.addPrestigePoints(-toPayWithPP);
                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), -toPayWithPP));
            }
        }

        return new EffectOutcome(deltas);
    }

}
