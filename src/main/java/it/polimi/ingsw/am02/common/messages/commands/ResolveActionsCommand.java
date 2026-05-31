package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

import java.util.List;

/**
 * In-game command requesting card acquisition for the current player.
 *
 * @param nickname    the nickname of the player issuing the command
 * @param selectedIDs the list of card IDs the player wishes to acquire
 */
public record ResolveActionsCommand(String nickname, List<String> selectedIDs) implements GameCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestResolveActions(clientId, selectedIDs);
    }
}