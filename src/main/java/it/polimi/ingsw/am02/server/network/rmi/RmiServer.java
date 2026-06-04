package it.polimi.ingsw.am02.server.network.rmi;

import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerFactory;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;
import it.polimi.ingsw.am02.server.network.NetworkServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * {@link NetworkServer} implementation for the RMI transport.
 *
 * <p>On {@link #start(int)}, attempts to reuse an existing RMI registry on the
 * given port (useful after a crash within the same JVM process); if none is
 * reachable, a new one is created. A {@link RmiServerFactory} is then exported
 * and bound to the registry under the name {@code "AM02-GameServer"}.
 *
 * <p>Each connecting client invokes {@link RmiServerFactory#registerClient},
 * which instantiates an {@link RmiClientHandler} and exports it as a
 * {@link RmiServerRemote} stub, establishing the two-way RMI communication
 * channel.
 */
public class RmiServer implements NetworkServer {

    private Registry registry;
    private RmiServerFactory factory;

    /**
     * Starts the RMI server on the specified port.
     *
     * <p>Tries to reuse an existing registry on {@code port}; creates a new one
     * if none responds. Exports and registers the {@link RmiServerFactory} stub
     * so that clients can obtain a per-connection {@link RmiServerRemote} stub.
     *
     * @param port the RMI registry port to bind to
     */
    @Override
    public void start(int port) {
        try {
            try {
                registry = LocateRegistry.getRegistry(port);
                registry.list();
            } catch (Exception e) {
                registry = LocateRegistry.createRegistry(port);
            }

            factory = clientCallback -> {
                RmiClientHandler handler = new RmiClientHandler(clientCallback);
                return (RmiServerRemote) UnicastRemoteObject.exportObject(handler, 1099);
            };

            RmiServerFactory stub = (RmiServerFactory) UnicastRemoteObject.exportObject(factory, 1099);
            registry.rebind("AM02-GameServer", stub);

            System.out.println("RMI Server started on port " + port);

        } catch (Exception e) {
            System.err.println("RMI Server start error: " + e.getMessage());
        }
    }

    /**
     * Stops the RMI server by unbinding the factory from the registry and
     * unexporting the registry object.
     */
    @Override
    public void stop() {
        try {
            if (registry != null) {
                registry.unbind("AM02-GameServer");
                UnicastRemoteObject.unexportObject(registry, true);
            }
        } catch (Exception e) {
            System.err.println("RMI Server stop error: " + e.getMessage());
        }
    }
}