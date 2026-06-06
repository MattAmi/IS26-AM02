package it.polimi.ingsw.am02.common.messages.commands.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Heartbeat probe sent by the client to verify server liveness.
 *
 * <p>The server replies with a
 * {@link it.polimi.ingsw.am02.common.messages.events.heartbeat.PongEvent}.
 * Intercepted entirely at the transport layer
 * ({@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler});
 * {@link #apply} must never be reached in normal operation.
 */
public record PingCommand() implements HeartbeatCommand {

    /**
     * Must never be called: this command is intercepted at the transport layer
     * before {@code apply} is invoked. If this method is reached, it indicates
     * a bug in the dispatch logic.
     *
     * @param manager  unused
     * @param clientId unused
     * @throws UnsupportedOperationException always
     */
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        throw new UnsupportedOperationException(
                "PingCommand is handled at transport level and must never reach apply()");
    }
}