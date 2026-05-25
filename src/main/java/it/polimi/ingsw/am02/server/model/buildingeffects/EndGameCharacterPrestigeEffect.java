package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.List;

public class EndGameCharacterPrestigeEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final CharacterType type;
    final int bonusPP;

    public EndGameCharacterPrestigeEffect(Player owner, CharacterType type, int bonusPP) {
        this.owner = owner;
        this.type = type;
        this.bonusPP = bonusPP;
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int bonus = bonusPP * tribu.getCharacterCount(type);

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
