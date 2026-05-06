package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record UsernameResultEvent(String username, boolean isValid, String reason) implements LobbyEvent {
    @Override public void applyTo(LobbyModel model) { model.updateUsernameResult(username, isValid, reason); }
}