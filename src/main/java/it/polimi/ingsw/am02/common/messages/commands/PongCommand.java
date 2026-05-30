package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Heartbeat response sent by the client in reply to a
 * {@link it.polimi.ingsw.am02.common.messages.events.game.PingEvent}.
 * Handled at the transport layer before {@link #apply} is called;
 * {@link #apply} is therefore a no-op.
 */
public record PongCommand() implements LobbyCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        // Heartbeat — handled at transport level before apply() is called.
    }
}