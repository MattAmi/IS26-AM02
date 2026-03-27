package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.*;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;

import java.util.List;


public class PrestigePointMultiplierPerTypeEffect implements BuildingEffect, PhaseObserver {
    final int multiplier;
    final CharacterType type;
    final Tribu tribu;
    public PrestigePointMultiplierPerTypeEffect(int multiplier, CharacterType type, Tribu tribu) {
        this.multiplier = multiplier;
        this.type = type;
        this.tribu = tribu;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    @Override
    public void onPhaseChanged(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME){
            int bonus = tribu.getPPBuilders()*(multiplier-1);
            tribu.addPrestigePoints(bonus);
        }
    }
}
