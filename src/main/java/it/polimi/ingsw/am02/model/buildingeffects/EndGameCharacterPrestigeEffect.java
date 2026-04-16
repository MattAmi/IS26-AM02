package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.model.Tribu;

public class EndGameCharacterPrestigeEffect implements BuildingEffect, PhaseObserver {
    final Tribu tribu;
    final CharacterType type;
    final int bonusPP;

    public EndGameCharacterPrestigeEffect(Tribu tribu, CharacterType type, int bonusPP) {
        this.tribu = tribu;
        this.type = type;
        this.bonusPP = bonusPP;
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    @Override
    public void onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME){
            tribu.addPrestigePoints(bonusPP * tribu.getCharacterCount(type));
        }
    }
}
