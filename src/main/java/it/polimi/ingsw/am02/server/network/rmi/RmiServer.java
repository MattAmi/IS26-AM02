package it.polimi.ingsw.am02.server.network.rmi;

import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerFactory;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;
import it.polimi.ingsw.am02.server.network.NetworkServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RmiServer implements NetworkServer {

    private Registry registry;
    private RmiServerFactory factory;

    @Override
    public void start(int port) {
        try {
            factory = new RmiServerFactory() {
                @Override
                public RmiServerRemote registerClient(RmiClientRemote clientCallback) throws RemoteException {
                    System.out.println("[RMI] New client connected! Assigning Handler...");
                    RmiClientHandler handler = new RmiClientHandler(clientCallback);
                    return (RmiServerRemote) UnicastRemoteObject.exportObject(handler, 0);
                }
            };

            RmiServerFactory stub = (RmiServerFactory) UnicastRemoteObject.exportObject(factory, 0);

            registry = LocateRegistry.createRegistry(port);
            registry.rebind("AM02-GameServer", stub);

            System.out.println("RMI Server started. Reception listening on port " + port);

        } catch (Exception e) {
            System.err.println("RMI Server start error: " + e.getMessage());
        }
    }

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