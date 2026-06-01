package it.polimi.ingsw.am02.server.model.effect;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.player.Player;

/**
 * A no-op {@link CharacterEffect} used for character types that produce no immediate
 * resource change when drawn (e.g. Artists, or Hunters without a gigot).
 */
public class NoEffect implements CharacterEffect {

    /**
     * Produces no resource changes.
     *
     * @param player the player who drew the character (unused)
     * @return an empty {@link EffectOutcome}
     */
    @Override
    public EffectOutcome applyEffect(Player player) {
        return EffectOutcome.empty();
    }
}
