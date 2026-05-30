package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players that an event card has been resolved. */
public record EventResolvedEvent(String eventID, String eventName) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyEventResolved(eventID, eventName);
    }
}