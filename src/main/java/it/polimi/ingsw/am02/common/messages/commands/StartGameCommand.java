package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

public record StartGameCommand() implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        // The game starts automatically when the lobby is full — no action needed.
    }
}