package it.polimi.ingsw.am02.common.messages.events.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/**
 * Heartbeat response sent by the server in reply to a client
 * {@link it.polimi.ingsw.am02.common.messages.commands.heartbeat.PingCommand}.
 *
 * <p>Receipt of this event resets the client-side liveness timer in
 * {@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy}.
 * Handled at the transport layer before {@link #apply} is called;
 * {@link #apply} is therefore a no-op.
 */
public record PongEvent() implements HeartbeatEvent {

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