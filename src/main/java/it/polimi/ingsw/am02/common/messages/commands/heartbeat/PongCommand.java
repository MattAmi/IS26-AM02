package it.polimi.ingsw.am02.common.messages.commands.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * Heartbeat response sent by the client in reply to a server
 * {@link it.polimi.ingsw.am02.common.messages.events.heartbeat.PingEvent}.
 *
 * <p>Receipt of this command resets the server-side liveness timer in
 * {@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler}.
 * Intercepted entirely at the transport layer; {@link #apply} must never
 * be reached in normal operation.
 */
public record PongCommand() implements HeartbeatCommand {

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
                "PongCommand is handled at transport level and must never reach apply()");
    }
}