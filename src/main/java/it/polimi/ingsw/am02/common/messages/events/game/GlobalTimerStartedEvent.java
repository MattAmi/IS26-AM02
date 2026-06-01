package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies connected players that the global forfeit timer has been armed. */
public record GlobalTimerStartedEvent(long seconds) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGlobalTimerStarted(seconds);
    }
}
