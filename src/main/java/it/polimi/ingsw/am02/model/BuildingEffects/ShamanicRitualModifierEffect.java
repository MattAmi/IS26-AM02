package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.PhaseObserver;
import it.polimi.ingsw.am02.model.Tribu;

public class ShamanicRitualModifierEffect implements BuildingEffect, PhaseObserver {
    final Tribu tribu;
    final int bonusStars;

    public ShamanicRitualModifierEffect(Tribu tribu, int bonusStars) {
        this.tribu = tribu;
        this.bonusStars = bonusStars;
    }

    @Override
    public void accept(EffectVisitor v) {

    }

    @Override
    public void onPhaseChanged(PhaseType newPhase) {

    }
}
