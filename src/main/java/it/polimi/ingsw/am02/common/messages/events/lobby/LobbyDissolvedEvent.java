package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies the client that their current lobby has been dissolved. */
public record LobbyDissolvedEvent(String lobbyID) implements LobbyEvent {
    @Override
    public void apply(VirtualView view) {
        view.notifyLobbyDissolved(lobbyID);
    }
}