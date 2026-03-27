package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.PhaseObserver;
import it.polimi.ingsw.am02.model.Tribu;

public class EndGameCharacterPrestigeEffect implements BuildingEffect, PhaseObserver {
    final Tribu tribu;
    final CharacterType type;
    final int BonusPP;

    public EndGameCharacterPrestigeEffect(Tribu tribu, CharacterType type, int bonusPP) {
        this.tribu = tribu;
        this.type = type;
        BonusPP = bonusPP;
    }


    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    @Override
    public void onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME){
            tribu.addPrestigePoints (BonusPP * tribu.getCharacterCount(type));
        }
    }
}
