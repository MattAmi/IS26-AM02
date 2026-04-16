package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.model.PhaseObserver;
import it.polimi.ingsw.am02.model.Tribu;

public class FlatPrestigeBonusEffect implements BuildingEffect, PhaseObserver {

    final Tribu tribu;
    final int bonusPP;

    public FlatPrestigeBonusEffect(Tribu tribu, int bonusPP) {
        this.tribu = tribu;
        this.bonusPP = bonusPP;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    @Override
    public void onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME){
            tribu.addPrestigePoints(bonusPP);
        }
    }
}
