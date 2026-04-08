package it.polimi.ingsw.am02.model;

public interface EffectVisitor {

    void visitPhaseObserver(PhaseObserver phaseObserver);
    void visitTribuObserver(TribuObserver tribuObserver);
    void visitEventObserver(EventObserver eventObserver);
}
