package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Event effect for the Shamanic Ritual event.
 * The player(s) with the most shaman stars gain a prestige bonus;
 * the player(s) with the fewest stars lose prestige points (unless immune).
 */
public class ShamanicRitualEffect implements EventEffect {

    final int majorityBonus;
    final int minorityBonus;

    /**
     * @param maxbonus prestige points awarded to the player(s) with the most shaman stars
     * @param minbonus prestige points deducted from the player(s) with the fewest shaman stars
     */
    public ShamanicRitualEffect(int maxbonus, int minbonus) {
        this.majorityBonus = maxbonus;
        this.minorityBonus = minbonus;
    }

    /**
     * Resolves the Shamanic Ritual for all players.
     * Immunity (granted by a building effect) prevents the minority penalty.
     *
     * @param players all active players
     * @return an {@link EffectOutcome} with prestige-point deltas
     */
    @Override
    public EffectOutcome applyEffect(List<Player> players) {
        List<ResourceDelta> deltas = new ArrayList<>();
        int maxStars = 0;
        int minStars = Integer.MAX_VALUE;

        for (Player player : players) {
            int stars = player.getTribu().getShamanStars();
            if (stars > maxStars) maxStars = stars;
            if (stars < minStars) minStars = stars;
        }

        int finalMaxStars = maxStars;
        int maxCount = Math.toIntExact(players.stream()
                .filter(p -> p.getTribu().getShamanStars() == finalMaxStars)
                .count());

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            String nickname = player.getNickname();
            int stars = tribu.getShamanStars();
            boolean isExclusiveWinner = (stars == maxStars && maxCount == 1);

            if (stars == maxStars) {
                tribu.addPrestigePoints(majorityBonus);

                tribu.setLastEventBonusReceived(isExclusiveWinner ? majorityBonus : 0);

                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), majorityBonus));
            } else {
                tribu.setLastEventBonusReceived(0);
            }

            // Caso Minoranza (solo se non immune)
            if (stars == minStars && !tribu.isImmune()) {
                tribu.addPrestigePoints(-minorityBonus);
                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), -minorityBonus));
            }
        }
        return new EffectOutcome(deltas);
    }

}