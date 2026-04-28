package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.model.GameRegistry;
import it.polimi.ingsw.am02.server.network.NetworkServer;
import it.polimi.ingsw.am02.server.network.NetworkServerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SERVER IN ASCOLTO ===");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");

        // 1. Inizializzazione Dominio
        GameRegistry.getInstance();
        ControllerManager manager = ControllerManager.getInstance();

        // 2. RECUPERO PERSISTENZA (Cerca i file di log e ripristina le partite interrotte)
        Path logsDir = Paths.get("logs"); // Assicurati che questa cartella esista o venga creata dal logger
        manager.recoverGames(logsDir);

        // 3. Avvio Server
        NetworkServer rmiServer = NetworkServerFactory.create(NetworkType.RMI);
        rmiServer.start(1099);

        Thread.currentThread().join();
    }
}