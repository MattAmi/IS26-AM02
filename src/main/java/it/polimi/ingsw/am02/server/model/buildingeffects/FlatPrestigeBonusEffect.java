package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.List;

public class FlatPrestigeBonusEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final int bonusPP;

    public FlatPrestigeBonusEffect(Player owner, int bonusPP) {
        this.owner = owner;
        this.bonusPP = bonusPP;
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

            tribu.addPrestigePoints(bonusPP);

            return new EffectOutcome(List.of(
                    new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), bonusPP)
            ));
        }

        return EffectOutcome.empty();
    }
}
