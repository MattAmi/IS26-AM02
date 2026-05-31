package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players that a specific player has disconnected. */
public record PlayerDisconnectedEvent(String nickname) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyPlayerDisconnected(nickname);
    }
}