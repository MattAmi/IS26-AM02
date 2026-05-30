package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies that the lobby game has started and supplies the game ID. */
public record GameStartedEvent(String gameID) implements LobbyEvent {
    @Override
    public void apply(VirtualView view) {
        view.notifyGameStarted(gameID);
    }
}