package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record LobbyDissolvedEvent(String lobbyID) implements LobbyEvent {
    @Override public void applyTo(LobbyModel model) { model.updateLobbyDissolved(); }
}