package it.polimi.ingsw.am02.common.messages.commands.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Heartbeat probe sent by the client to verify server liveness.
 *
 * <p>The server replies with a
 * {@link it.polimi.ingsw.am02.common.messages.events.heartbeat.PongEvent}.
 * Both directions are handled entirely at the transport layer
 * ({@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler});
 * {@link #apply} is therefore a no-op and is never reached in normal operation.
 */
public record PingCommand() implements HeartbeatCommand {

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
