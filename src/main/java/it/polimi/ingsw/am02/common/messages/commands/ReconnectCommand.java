package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record ReconnectCommand(String nickname, String gameId) implements Command {
    /**
     * Applies the reconnect command, supplying the caller's {@link VirtualView}
     * so the manager can update the player's notification target.
     *
     * @param manager  the controller manager
     * @param clientId the new connection's client identifier
     * @param view     the {@link VirtualView} associated with this connection
     */
    public void apply(VirtualControllerManager manager, String clientId,
                      it.polimi.ingsw.am02.common.interfaces.VirtualView view) {
        manager.requestReconnect(clientId, view, nickname, gameId);
    }

    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        // Should not be called directly — use apply(manager, clientId, view) instead.
        throw new UnsupportedOperationException(
                "ReconnectCommand requires a VirtualView; use apply(manager, clientId, view)");
    }
}
