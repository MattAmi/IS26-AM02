package it.polimi.ingsw.am02.common.messages.events.game;


import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record TotemPlacedEvent(String nickname, char tileID) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyTotemPlaced(nickname, tileID);
    }
}