package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Command requesting creation of a new lobby with the specified player capacity.
 *
 * @param numPlayers the desired number of players (2–5)
 */
public record CreateLobbyCommand(int numPlayers) implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestCreateLobby(clientId, numPlayers);
    }

}