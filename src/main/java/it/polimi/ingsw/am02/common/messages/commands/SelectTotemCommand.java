package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Command requesting totem selection within the current lobby.
 *
 * @param color the desired totem color
 */
public record SelectTotemCommand(Totem color) implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestSelectTotem(clientId, color);
    }
}