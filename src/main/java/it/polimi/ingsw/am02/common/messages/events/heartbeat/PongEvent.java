package it.polimi.ingsw.am02.common.messages.events.heartbeat;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/**
 * Heartbeat response sent by the server in reply to a client
 * {@link it.polimi.ingsw.am02.common.messages.commands.heartbeat.PingCommand}.
 *
 * <p>Receipt of this event resets the client-side liveness timer in
 * {@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy}.
 * Intercepted entirely at the transport layer; {@link #apply} must never
 * be reached in normal operation.
 */
public record PongEvent() implements HeartbeatEvent {

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
                "PongEvent is handled at transport level and must never reach apply()");
    }
}