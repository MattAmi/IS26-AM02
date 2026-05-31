package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import java.util.List;

/** Notifies all players of a phase transition. */
public record PhaseChangedEvent(PhaseType phase, String currentPlayer, List<String> resolutionOrder) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyPhaseChanged(phase, currentPlayer, resolutionOrder);
    }
}