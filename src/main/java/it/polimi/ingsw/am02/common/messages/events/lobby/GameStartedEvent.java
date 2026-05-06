package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record GameStartedEvent(String gameID) implements LobbyEvent {
    @Override
    public void apply(VirtualView view) {
        view.notifyGameStarted(gameID);
    }
}