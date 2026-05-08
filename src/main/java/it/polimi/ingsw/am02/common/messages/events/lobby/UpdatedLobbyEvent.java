package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record UpdatedLobbyEvent(LobbyInfo lobby) implements LobbyEvent {
    @Override
    public void apply(VirtualView view) {
        view.notifyCurrentLobbyUpdated(lobby);
    }
}