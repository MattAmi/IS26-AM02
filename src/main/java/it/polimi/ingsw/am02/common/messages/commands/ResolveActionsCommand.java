package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

import java.util.List;

public record ResolveActionsCommand(String nickname, List<String> selectedIDs) implements GameCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestResolveActions(clientId, selectedIDs);
    }
}