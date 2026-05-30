package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players that a player returned their totem to the turn-order tile. */
public record TotemReturnedEvent(String nickname, int turnOrderPosition) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyTotemReturned(nickname, turnOrderPosition);
    }
}