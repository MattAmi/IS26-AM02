package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

public record MoveTotemCommand(String nickname, char tileID) implements GameCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestMoveTotem(clientId, tileID);
    }
}