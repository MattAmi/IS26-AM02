package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;

public class NoEffect implements CharacterEffect {

    //intentionally left empty, it serves in the cases where the character doesn't have a direct impact once drawn
    //for example when i draw a hunter without a gigot, it doesn't have any immediate effect
    @Override
    public EffectOutcome applyEffect(Player player) {
        return EffectOutcome.empty();
    }
}
