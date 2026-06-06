package it.polimi.ingsw.am02.common.messages.events.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/**
 * Heartbeat probe sent by the server to verify client liveness.
 *
 * <p>The client replies with a
 * {@link it.polimi.ingsw.am02.common.messages.commands.heartbeat.PongCommand}.
 * Both directions are handled entirely at the transport layer
 * ({@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy});
 * {@link #apply} is therefore a no-op and is never reached in normal operation.
 */
public record PingEvent() implements HeartbeatEvent {

    /**
     * No-op: this event is intercepted and handled at the transport layer
     * before {@code apply} is invoked.
     *
     * @param view unused
     */
    @Override
    public void apply(VirtualView view) {
        // Heartbeat — handled at transport level before apply() is called.
    }
}
