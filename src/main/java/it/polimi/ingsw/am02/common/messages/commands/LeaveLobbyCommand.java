package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Command requesting that the client leave their current lobby.
 */
public record LeaveLobbyCommand() implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestLeaveLobby(clientId);
    }
}