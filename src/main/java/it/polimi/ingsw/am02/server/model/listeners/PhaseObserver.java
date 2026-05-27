package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;

/**
 * Observer notified whenever the game transitions between phases.
 * Implemented by building effects that react to specific {@link PhaseType} transitions
 * (e.g. end-of-round, end-of-game).
 */
public interface PhaseObserver {

    /**
     * Called each time the game enters a new phase.
     *
     * @param newPhase the phase the game is transitioning into
     * @return an {@link EffectOutcome} describing any resource changes triggered by
     *         this phase transition, or an empty outcome if no changes occurred
     */
    EffectOutcome onPhaseChange(PhaseType newPhase);
}