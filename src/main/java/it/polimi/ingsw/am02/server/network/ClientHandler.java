package it.polimi.ingsw.am02.server.network;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/**
 * Server-side per-client connection handler.
 *
 * <p>Extends {@link VirtualView} with a lifecycle method for tearing down
 * the underlying transport. Concrete implementations exist for each supported
 * network technology: {@link it.polimi.ingsw.am02.server.network.rmi.RmiClientHandler}
 * (RMI) and {@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler}
 * (Socket).
 *
 * <p>Both implementations guarantee that {@link VirtualView} notifications
 * never block {@link it.polimi.ingsw.am02.server.controller.GameController}
 * while it holds its lock.
 */
public interface ClientHandler extends VirtualView {

    /**
     * Tears down the connection and notifies
     * {@link it.polimi.ingsw.am02.server.controller.ControllerManager} of the
     * disconnection. Must be idempotent: repeated calls after the first are no-ops.
     */
    void disconnect();
}