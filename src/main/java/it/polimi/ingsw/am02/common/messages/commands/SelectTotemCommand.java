package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

public record SelectTotemCommand(Totem color) implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestSelectTotem(clientId, color);
    }
}