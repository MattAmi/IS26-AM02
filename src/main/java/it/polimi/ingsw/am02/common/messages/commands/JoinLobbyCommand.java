package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Command requesting that the client join an existing lobby.
 *
 * @param lobbyID the identifier of the target lobby
 */
public record JoinLobbyCommand(String lobbyID) implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestJoinLobby(clientId, lobbyID);
    }
}