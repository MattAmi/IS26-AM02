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

        for (Player player : players) {
            player.getTribu().setLastEventBonusReceived(0);
        }

        int maxStars = 0;
        int minStars = Integer.MAX_VALUE;

        for (Player player : players) {
            int effectiveStars = player.getTribu().getShamanStars();
            if (effectiveStars > maxStars) maxStars = effectiveStars;
            if (effectiveStars < minStars) minStars = effectiveStars;
        }

        int finalMaxStars = maxStars;
        long maxCount = players.stream()
                .filter(p -> p.getTribu().getShamanStars() == finalMaxStars)
                .count();

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            int effectiveStars = tribu.getShamanStars();
            boolean isExclusiveWinner = (effectiveStars == maxStars && maxCount == 1);

            if (effectiveStars == maxStars) {
                tribu.addPrestigePoints(majorityBonus);
                tribu.setLastEventBonusReceived(isExclusiveWinner ? majorityBonus : 0);
            } else {
                tribu.setLastEventBonusReceived(0);
            }

            if (effectiveStars == minStars && !tribu.isImmune()) {
                tribu.addPrestigePoints(minorityBonus);
            }
        }
    }

}