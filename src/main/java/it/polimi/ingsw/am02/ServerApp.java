package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.model.GameRegistry;
import it.polimi.ingsw.am02.server.network.rmi.RmiServer;
import it.polimi.ingsw.am02.server.network.socket.SocketServer;

public class ServerApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Server Initialization ===");

        System.setProperty("java.rmi.server.hostname", "127.0.0.1");

        // Singletons init
        GameRegistry.getInstance();
        ControllerManager.getInstance();

        RmiServer rmiServer = new RmiServer();
        rmiServer.start(1099);
        System.out.println("[RMI] Server in ascolto sulla porta 1099");

        SocketServer socketServer = new SocketServer();
        socketServer.start(1234);
        System.out.println("[SOCKET] Server in ascolto sulla porta 1234");

        Thread.currentThread().join();
    }
}