package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.model.GameRegistry;
import it.polimi.ingsw.am02.server.network.NetworkServer;
import it.polimi.ingsw.am02.server.network.NetworkServerFactory;

public class ServerApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SERVER IN ASCOLTO ===");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");

        // Inizializzazione Dominio
        GameRegistry.getInstance();
        ControllerManager.getInstance();

        // Avvio contemporaneo di entrambi i moduli di rete
        NetworkServer rmiServer = NetworkServerFactory.create(NetworkType.RMI);
        rmiServer.start(1099);

        // NetworkServer socketServer = NetworkServerFactory.create(NetworkType.SOCKET);
        // socketServer.start(1234); // Decommentare quando Raed avrà finito i socket

        Thread.currentThread().join();
    }
}