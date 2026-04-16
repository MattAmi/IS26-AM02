package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;

public interface EffectVisitor {
    void visitPhaseObserver(PhaseObserver phaseObserver);
    void visitTribuObserver(TribuObserver tribuObserver);
    void visitEventObserver(EventObserver eventObserver);

}
