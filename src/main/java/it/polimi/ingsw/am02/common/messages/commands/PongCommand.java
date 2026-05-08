package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

public record PongCommand() implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        // Heartbeat — handled at transport level before apply() is called.
    }
}