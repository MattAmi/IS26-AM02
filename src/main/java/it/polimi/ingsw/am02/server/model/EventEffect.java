package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;

import java.util.List;

/**
 * Strategy interface for the game-wide effect applied when an event card is resolved.
 */
public interface EventEffect {

    /**
     * Applies this event's effect to all players.
     *
     * @param players all active players, in any order
     * @return an {@link EffectOutcome} containing all resource deltas produced by this effect
     */
    EffectOutcome applyEffect(List<Player> players);
}
