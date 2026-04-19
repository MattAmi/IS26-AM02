package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;

public interface CharacterEffect {
    EffectOutcome applyEffect(Player player);
}
