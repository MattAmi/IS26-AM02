package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record EventResolvedEvent(String eventID, String eventName) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyEventResolved(eventID, eventName);
    }
}