package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.BuildingEffects.EndGamePrestigeRewardEffect;

public interface EffectVisitor {

    public void visitPhaseObserver(PhaseObserver phaseObserver);
    public void visitTribuObserver(TribuObserver tribuObserver);
    public void visitEventObserver(EventObserver eventObserver);
}
