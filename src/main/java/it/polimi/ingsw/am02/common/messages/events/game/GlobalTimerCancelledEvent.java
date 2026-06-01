package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies connected players that the global forfeit timer has been cancelled. */
public record GlobalTimerCancelledEvent() implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGlobalTimerCancelled();
    }
}