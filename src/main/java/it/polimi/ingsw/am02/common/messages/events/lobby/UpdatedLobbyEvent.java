package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record UpdatedLobbyEvent(LobbyInfo lobby) implements LobbyEvent {
    @Override public void applyTo(LobbyModel model) { model.updateCurrentLobby(lobby); }
}