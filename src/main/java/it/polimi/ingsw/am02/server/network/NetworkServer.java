package it.polimi.ingsw.am02.server.network;

/**
 * Abstraction for a network server that accepts client connections on a
 * given port and dispatches them to the appropriate
 * {@link ClientHandler} implementation.
 *
 * <p>Concrete implementations are provided for each supported transport:
 * <ul>
 *   <li>{@link it.polimi.ingsw.am02.server.network.socket.SocketServer} —
 *       accepts TCP connections and creates
 *       {@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler}
 *       instances.</li>
 *   <li>{@link it.polimi.ingsw.am02.server.network.rmi.RmiServer} —
 *       binds an RMI registry and exports the remote stub.</li>
 * </ul>
 *
 * <p>Instances are obtained through {@link NetworkServerFactory} and should
 * not be constructed directly.
 */
public interface NetworkServer {

    /**
     * Starts the server and begins accepting client connections on the
     * specified port.
     *
     * <p>The method may block until the server is ready to accept
     * connections, but must not block the calling thread indefinitely;
     * the actual accept loop should run on a dedicated daemon thread.
     *
     * @param port the TCP port (Socket) or RMI registry port (RMI) to
     *             listen on; must be in the range 1–65535
     * @throws RuntimeException if the server cannot bind to the given port
     *                          or if initialization fails
     */
    void start(int port);

    /**
     * Gracefully shuts down the server, stopping the accept loop and
     * releasing all bound ports and resources.
     *
     * <p>Already-connected clients are not forcibly disconnected by this
     * call; their {@link ClientHandler#disconnect()} lifecycle is managed
     * independently.
     */
    void stop();
}