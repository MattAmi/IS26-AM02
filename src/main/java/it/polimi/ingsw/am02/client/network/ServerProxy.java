package it.polimi.ingsw.am02.client.network;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.common.interfaces.VirtualServer;

/**
 * Client-side abstraction for the connection to the game server.
 *
 * <p>Extends {@link VirtualServer} with lifecycle methods ({@link #connect()},
 * {@link #disconnect()}, {@link #isConnected()}) and a wiring hook
 * ({@link #setClientController}) that lets the implementing proxy instantiate
 * its {@link ClientNetworkDispatcher} once the controller is available.
 *
 * <p>Concrete implementations are provided for RMI
 * ({@link it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy}) and Socket
 * ({@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy}).
 * Instances are created via {@link ServerProxyFactory}.
 *
 * @see ServerProxyFactory
 */
public interface ServerProxy extends VirtualServer {

    /**
     * Establishes the physical connection to the server.
     *
     * <p>For RMI, performs the registry lookup and exports this object as a
     * remote stub. For Socket, opens the TCP connection and starts the reader
     * and ping threads. Must be called after {@link #setClientController} and
     * before any outbound request is issued.
     *
     * @throws Exception if the connection cannot be established
     */
    void connect() throws Exception;

    /**
     * Closes the connection and releases all associated resources (threads,
     * sockets, RMI stubs).
     *
     * <p>Implementations must set an intentional-disconnect flag so that
     * background threads do not trigger an automatic reconnection attempt.
     */
    void disconnect();

    /**
     * Returns whether the proxy currently has an active connection to the server.
     *
     * @return {@code true} if connected, {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Wires this proxy to the given {@link ClientController} and creates the
     * {@link ClientNetworkDispatcher} that routes inbound server notifications
     * to the appropriate client-side model.
     *
     * <p>Must be called before {@link #connect()} so that inbound events
     * are dispatched correctly as soon as the connection is live.
     *
     * @param controller the client controller that owns the active model
     */
    void setClientController(ClientController controller);
}