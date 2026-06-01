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
 * Event effect for the Cave Paintings event.
 * Players who have at least the required number of Artist characters gain prestige
 * points per Artist; players who fall short lose prestige points.
 */
public class CavePaintingsEffect implements EventEffect {

    final int minArtists; //minimum number to profit from the event
    final int ppMalusIfFailed;
    final int bonusPerArtist;

    /**
     * @param minArtists      minimum Artist count needed to benefit from the event
     * @param malusIfFailed   prestige points lost if the player has fewer than {@code minArtists} Artists
     * @param bonusPerArtist  prestige points gained per Artist if the threshold is met
     */
    public CavePaintingsEffect(int minArtists, int malusIfFailed, int bonusPerArtist) {
        this.minArtists = minArtists;
        this.ppMalusIfFailed = malusIfFailed;
        this.bonusPerArtist = bonusPerArtist;
    }

    /**
     * Applies the Cave Paintings outcome to every player.
     *
     * @param players all active players
     * @return an {@link EffectOutcome} with prestige-point deltas for each player
     */
    @Override
    public EffectOutcome applyEffect(List<Player> players) {
        List<ResourceDelta> deltas = new ArrayList<>();

        for (Player player : players) {
            Tribu tribu = player.getTribu();
            String nickname = player.getNickname();

            int numOfArtists = tribu.getCharacterCount(CharacterType.ARTIST);

            if (numOfArtists >= minArtists) {
                int bonus = bonusPerArtist * numOfArtists;
                tribu.addPrestigePoints(bonus);
                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), bonus));
            } else {
                tribu.addPrestigePoints(-ppMalusIfFailed);
                deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), -ppMalusIfFailed));
            }
        }
        return new EffectOutcome(deltas);
    }

}