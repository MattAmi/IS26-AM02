package it.polimi.ingsw.am02.common.messages.events.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/**
 * Heartbeat probe sent by the server to verify client liveness.
 *
 * <p>The client replies with a
 * {@link it.polimi.ingsw.am02.common.messages.commands.heartbeat.PongCommand}.
 * Intercepted entirely at the transport layer
 * ({@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy});
 * {@link #apply} must never be reached in normal operation.
 */
public record PingEvent() implements HeartbeatEvent {

    /**
     * Must never be called: this event is intercepted at the transport layer
     * before {@code apply} is invoked. If this method is reached, it indicates
     * a bug in the dispatch logic.
     *
     * @param view unused
     * @throws UnsupportedOperationException always
     */
    @Override
    public void apply(VirtualView view) {
        throw new UnsupportedOperationException(
                "PingEvent is handled at transport level and must never reach apply()");
    }
}