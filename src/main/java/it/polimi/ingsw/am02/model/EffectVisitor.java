package it.polimi.ingsw.am02.model;

public interface EffectVisitor {

    public void visitPhaseObserver(PhaseObserver o);
    public void visitTribuObserver(TribuObserver o);

}
