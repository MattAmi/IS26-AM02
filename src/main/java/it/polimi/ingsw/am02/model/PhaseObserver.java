package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.common.enumerations.PhaseType;

public interface PhaseObserver {
    void onPhaseChange(PhaseType newPhase);

}