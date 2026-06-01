package it.polimi.ingsw.am02.server.model.effect;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.player.Player;

/**
 * Strategy interface for the immediate effect applied to a player's tribe
 * when a character card is drawn.
 */
public interface CharacterEffect {

    /**
     * Applies this effect to the owner's tribe.
     *
     * @param player the player who drew the character card
     * @return an {@link EffectOutcome} describing the resource deltas produced
     */
    EffectOutcome applyEffect(Player player);
}
