package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;

import java.util.ArrayList;
import java.util.List;

public class HuntEffect implements EventEffect {

    //Attributi
    final int foodPerHunter;
    final int ppPerHunter;

    //Costruttore
    public HuntEffect(int foodPerHunter, int ppPerHunter) {
        this.foodPerHunter = foodPerHunter;
        this.ppPerHunter = ppPerHunter;
    }

    @Override
    public EffectOutcome applyEffect(List<Player> players) {
        List<ResourceDelta> deltas = new ArrayList<>();

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            int numOfHunters = tribu.getCharacterCount(CharacterType.HUNTER);

            if (numOfHunters > 0) {
                int foodGained = foodPerHunter * numOfHunters;
                int ppGained = ppPerHunter * numOfHunters;

                tribu.addFoodPoints(foodGained);
                tribu.addPrestigePoints(ppGained);

                String nickname = player.getNickname();

                deltas.add(new ResourceDelta(nickname, ResourceType.FOOD, tribu.getFoodPoints(), foodGained));
                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), ppGained));
            }
        }
        return new EffectOutcome(deltas);
    }

}
