package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;

import java.util.List;

public interface EventEffect {
    EffectOutcome applyEffect(List<Player> players);
}
