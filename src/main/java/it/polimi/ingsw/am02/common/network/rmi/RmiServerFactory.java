package it.polimi.ingsw.am02.common.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI remote interface acting as the server-side entry point for new client connections.
 *
 * <p>Clients look up this factory in the RMI registry and call
 * {@link #registerClient} to obtain a dedicated {@link RmiServerRemote} stub,
 * establishing the two-way communication channel.
 */
public interface RmiServerFactory extends Remote {

    /**
     * Registers a new client and returns a dedicated server-side stub for it.
     *
     * @param clientCallback the client's remote stub, used by the server to push
     *                       notifications back to the client
     * @return a {@link RmiServerRemote} stub bound to the new {@link
     *         it.polimi.ingsw.am02.server.network.rmi.RmiClientHandler} for this client
     * @throws RemoteException if the RMI call fails
     */
    RmiServerRemote registerClient(RmiClientRemote clientCallback) throws RemoteException;
}