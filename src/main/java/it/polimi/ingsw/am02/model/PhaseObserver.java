package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;

public interface PhaseObserver {
    public void onPhaseChange(PhaseType newPhase);
}