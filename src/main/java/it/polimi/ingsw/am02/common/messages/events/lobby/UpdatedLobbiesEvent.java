package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import java.util.List;

public record UpdatedLobbiesEvent(List<LobbyInfo> lobbies) implements LobbyEvent {
    @Override
    public void apply(VirtualView view) {
        view.notifyAvailableLobbiesUpdated(lobbies);
    }
}