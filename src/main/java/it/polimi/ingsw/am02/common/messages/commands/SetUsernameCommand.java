package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Command requesting that the server assign the given nickname to this client.
 *
 * @param username the desired nickname
 */
public record SetUsernameCommand(String username) implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestSetUsername(clientId, username);
    }
}