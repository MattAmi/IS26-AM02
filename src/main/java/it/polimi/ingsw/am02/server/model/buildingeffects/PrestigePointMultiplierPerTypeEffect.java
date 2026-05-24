package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.List;

public class PrestigePointMultiplierPerTypeEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final CharacterType type;
    final int multiplier;

    public PrestigePointMultiplierPerTypeEffect(Player owner, CharacterType type, int multiplier) {
        this.owner = owner;
        this.type = type;
        this.multiplier = multiplier;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int bonus = tribu.getPPBuilders() * (multiplier - 1);

            if (bonus == 0)
                return EffectOutcome.empty();

            tribu.addPrestigePoints(bonus);

            return new EffectOutcome(List.of(
                    new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), bonus)
            ));
        }

        return EffectOutcome.empty();
    }

}
