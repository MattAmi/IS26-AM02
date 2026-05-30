package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players that the active player has changed during totem placement. */
public record CurrentPlayerChangedEvent(String nextPlayer) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyCurrentPlayerChanged(nextPlayer);
    }
}