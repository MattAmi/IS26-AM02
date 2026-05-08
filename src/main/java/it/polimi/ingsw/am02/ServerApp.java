package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.model.GameRegistry;
import it.polimi.ingsw.am02.server.network.NetworkServer;
import it.polimi.ingsw.am02.server.network.NetworkServerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerApp {

    private static final int RMI_PORT    = 1099;
    private static final int SOCKET_PORT = 1100;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("      MESOS SERVER - INITIALIZING       ");
        System.out.println("========================================");

        try {
            // 1. Init Domain and Registry (authoritative source of truth)
            GameRegistry.getInstance();
            ControllerManager manager = ControllerManager.getInstance();

            // 2. Recovery System: Reload active games from persistent storage
            Path logsDir = Paths.get("logs");
            manager.recoverGames(logsDir);
            System.out.println("[INFO] Persistence check completed.");

            // 3. Start RMI Server
            NetworkServer rmiServer = NetworkServerFactory.create(NetworkType.RMI);
            Thread rmiThread = new Thread(() -> rmiServer.start(RMI_PORT), "NetworkServer-RMI");
            rmiThread.setDaemon(true);
            rmiThread.start();

            // 4. Start Socket Server
            NetworkServer socketServer = NetworkServerFactory.create(NetworkType.SOCKET);
            Thread socketThread = new Thread(() -> socketServer.start(SOCKET_PORT), "NetworkServer-Socket");
            socketThread.setDaemon(true);
            socketThread.start();

            System.out.println("[READY] RMI Server listening on port " + RMI_PORT);
            System.out.println("[READY] Socket Server listening on port " + SOCKET_PORT);
            System.out.println("----------------------------------------");

            // Keep the main thread alive to sustain daemon threads
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            System.err.println("[INFO] Server is shutting down...");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[FATAL] Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}