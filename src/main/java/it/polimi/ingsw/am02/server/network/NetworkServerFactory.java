package it.polimi.ingsw.am02.server.network;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.server.network.rmi.RmiServer;
import it.polimi.ingsw.am02.server.network.socket.SocketServer;

/**
 * Factory for creating {@link NetworkServer} instances based on the chosen
 * network technology.
 *
 * <p>This class centralizes the mapping between {@link NetworkType} values
 * and their concrete {@link NetworkServer} implementations, keeping the
 * server startup code decoupled from transport-specific constructors.
 *
 * <p>Usage example:
 * <pre>{@code
 * NetworkServer server = NetworkServerFactory.create(NetworkType.SOCKET);
 * server.start(12345);
 * }</pre>
 */
public class NetworkServerFactory {

    /** Prevents instantiation of this utility class. */
    private NetworkServerFactory() {}

    /**
     * Creates and returns a new {@link NetworkServer} for the given
     * {@link NetworkType}.
     *
     * @param type the network technology to use; must not be {@code null}
     * @return a new {@link SocketServer} if {@code type} is
     *         {@link NetworkType#SOCKET}, or a new {@link RmiServer} if
     *         {@code type} is {@link NetworkType#RMI}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public static NetworkServer create(NetworkType type) {
        return switch (type) {
            case SOCKET -> new SocketServer();
            case RMI    -> new RmiServer();
        };
    }
}