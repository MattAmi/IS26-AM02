package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.model.GameRegistry;
import it.polimi.ingsw.am02.server.network.rmi.RmiServer;

public class ServerApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SERVER RMI IN ASCOLTO ===");

        System.setProperty("java.rmi.server.hostname", "127.0.0.1");

        GameRegistry.getInstance();
        ControllerManager.getInstance();

        RmiServer server = new RmiServer();
        server.start(1099);

        Thread.currentThread().join();
    }
}