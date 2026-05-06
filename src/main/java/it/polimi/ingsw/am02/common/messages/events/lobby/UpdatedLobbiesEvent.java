package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.messages.events.Event;
import java.util.List;

public record UpdatedLobbiesEvent(List<LobbyInfo> lobbies) implements LobbyEvent {
    @Override public void applyTo(LobbyModel model) { model.updateAvailableLobbies(lobbies); }
}