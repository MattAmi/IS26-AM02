package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.server.network.rmi.RmiServer;

public class ServerApp {
    public static void main(String[] args) {
        System.out.println("=== SERVER RMI IN ASCOLTO ===");
        RmiServer server = new RmiServer();
        server.start(1099);

        // Il server resta attivo finché non lo stoppi manualmente
    }
}