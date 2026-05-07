package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

public record JoinLobbyCommand(String lobbyID) implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestJoinLobby(clientId, lobbyID);
    }
}