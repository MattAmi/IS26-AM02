package it.polimi.ingsw.am02.server.model.effect;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;

import java.util.ArrayList;
import java.util.List;

/**
 * Event effect for the Hunt event.
 * Each player gains food and prestige points proportional to the number of
 * Hunter characters in their tribe.
 */
public class HuntEffect implements EventEffect {

    final int foodPerHunter;
    final int ppPerHunter;

    /**
     * @param foodPerHunter food points gained per Hunter character
     * @param ppPerHunter   prestige points gained per Hunter character
     */
    public HuntEffect(int foodPerHunter, int ppPerHunter) {
        this.foodPerHunter = foodPerHunter;
        this.ppPerHunter = ppPerHunter;
    }

    /**
     * Applies the Hunt reward to every player.
     * Players with no Hunters receive no delta.
     *
     * @param players all active players
     * @return an {@link EffectOutcome} with food and prestige-point deltas
     */
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
