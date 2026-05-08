package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record PlayerReconnectedEvent(String nickname) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyPlayerReconnected(nickname);
    }
}