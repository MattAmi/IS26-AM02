package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players that the game was aborted by forfeit and declares the winner. */
public record GameAbortedEvent(String winner) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGameAborted(winner);
    }
}