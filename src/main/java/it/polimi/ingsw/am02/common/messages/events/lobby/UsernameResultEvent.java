package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record UsernameResultEvent(String username, boolean isValid, String reason) implements LobbyEvent {
    @Override
    public void apply(VirtualView view) {
        view.notifyUsernameResult(username, isValid, reason);
    }
}