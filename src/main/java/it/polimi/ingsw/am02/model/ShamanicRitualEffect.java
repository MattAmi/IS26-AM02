package it.polimi.ingsw.am02.model;

public class ShamanicRitualEffect implements EventEffect{

    //Attributi
    int majorityBonus;
    int minorityBonus;

    //Costruttore
    public ShamanicRitualEffect(int maxbonus, int minbonus) {
        this.majorityBonus = maxbonus;
        this.minorityBonus =  minbonus;
    }

    int maxStars = 0;
    int minStars = 0;
    //Metodi
    @Override
    public void applyEffect(Game game) {

        //determine the maximum and minimum stars
        for (Player player : game.getPlayers()) {
            Tribu tribu = player.getTribu();
            int stars = tribu.getShamanStars();

            int buildingStars = 0;

            //TODO : prendo eventuali stars dei building della tribu

            int effectiveStars = stars + buildingStars;
            if (effectiveStars > maxStars) {
                maxStars = effectiveStars;
            }
            if (effectiveStars < minStars) {
                minStars = effectiveStars;
            }
        }


        //give points to the one with the most stars, and take away points from
        // the one with the least stars(if he's not protected in some way)
        for (Player player : game.getPlayers()) {
            Tribu tribu = player.getTribu();
            int stars = tribu.getShamanStars();

            int buildingStars = 0;

            //TODO : prendo eventuali stars dei building della tribu

            int effectiveStars = stars + buildingStars;

            if (effectiveStars == maxStars) {
                tribu.addPrestigePoints(majorityBonus);
            }
            if (effectiveStars == minStars) {
                if(//la persona ha un building che lo protegge)
                //non paga niente
                else{
                    tribu.addPrestigePoints(minorityBonus);
                }
            }
        }


    }

}
