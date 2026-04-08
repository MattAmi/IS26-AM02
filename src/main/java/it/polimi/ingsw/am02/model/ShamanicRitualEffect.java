package it.polimi.ingsw.am02.model;

import java.util.List;

public class ShamanicRitualEffect implements EventEffect {

    //Attributi
    final int majorityBonus;
    final int minorityBonus;

    //Costruttore
    public ShamanicRitualEffect(int maxbonus, int minbonus) {
        this.majorityBonus = maxbonus;
        this.minorityBonus = minbonus;
    }

    //Metodi
    @Override
    public void applyEffect(List<Player> players) {

        int maxStars = 0;
        int minStars = Integer.MAX_VALUE;

        for (Player player : players) {
            int effectiveStars = player.getTribu().getShamanStars();
            if (effectiveStars > maxStars) maxStars = effectiveStars;
            if (effectiveStars < minStars) minStars = effectiveStars;
        }

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            int effectiveStars = tribu.getShamanStars();

            if (effectiveStars == maxStars) {
                tribu.addPrestigePoints(majorityBonus);
            }
            if (effectiveStars == minStars && !tribu.isImmune()) {
                tribu.addPrestigePoints(minorityBonus);
            }
        }

    }

}