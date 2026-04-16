package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.enumerations.PhaseType;

public interface PhaseObserver {
    void onPhaseChange(PhaseType newPhase);

}