package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;

/**
 * Visitor interface used to register {@link BuildingEffect} implementations
 * with the correct game-level observer list.
 *
 * <p>Each concrete building effect implements {@link BuildingEffect#accept(EffectVisitor)}
 * and calls the appropriate {@code visit*} method, which {@link RegistrationVisitor}
 * then uses to attach the effect to the right observer list.
 */
public interface EffectVisitor {

    /**
     * Registers a building effect that reacts to game phase transitions.
     *
     * @param phaseObserver the effect to attach as a {@link PhaseObserver}
     */
    void visitPhaseObserver(PhaseObserver phaseObserver);

    /**
     * Registers a building effect that reacts to character insertions in the tribe.
     *
     * @param tribuObserver the effect to attach as a {@link TribuObserver}
     */
    void visitTribuObserver(TribuObserver tribuObserver);

    /**
     * Registers a building effect that reacts to event card resolutions.
     *
     * @param eventObserver the effect to attach as an {@link EventObserver}
     */
    void visitEventObserver(EventObserver eventObserver);

}
