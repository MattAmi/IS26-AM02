package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record GameAbortedEvent(String winner) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGameAborted(winner);
    }
}