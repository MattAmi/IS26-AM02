package it.polimi.ingsw.am02.common.messages.commands.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Heartbeat response sent by the client in reply to a server
 * {@link it.polimi.ingsw.am02.common.messages.events.heartbeat.PingEvent}.
 *
 * <p>Handled at the transport layer before {@link #apply} is called;
 * {@link #apply} is therefore a no-op.
 */
public record PongCommand() implements HeartbeatCommand {

    /**
     * No-op: this command is intercepted and handled at the transport layer
     * before {@code apply} is invoked.
     *
     * @param manager  unused
     * @param clientId unused
     */
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        // Heartbeat — handled at transport level before apply() is called.
    }
}
