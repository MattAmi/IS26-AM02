package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.model.Tribu;

public class EndGameFullSetPPEffect implements BuildingEffect, PhaseObserver{
    final int bonusPP;
    final Tribu tribu;

    public EndGameFullSetPPEffect(int bonusPP, Tribu tribu) {
        this.bonusPP = bonusPP;
        this.tribu = tribu;
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    @Override
    public void onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME){
            int nSet = Integer.MAX_VALUE;

            for (CharacterType type : CharacterType.values()) {
                int count = tribu.getCharacterCount(type);
                if (count < nSet) {
                    nSet = count;
                }
            }
            tribu.addPrestigePoints(nSet*bonusPP);
        }
    }

}
