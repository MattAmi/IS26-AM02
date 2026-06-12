package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.common.NetworkDefaults;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.model.GameRegistry;
import it.polimi.ingsw.am02.server.network.NetworkServer;
import it.polimi.ingsw.am02.server.network.NetworkServerFactory;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ServerApp {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("      MESOS SERVER - INITIALIZING       ");
        System.out.println("========================================");

        Scanner scanner = new Scanner(System.in);

        try {
            // --- NETWORK CONFIGURATION ---
            String localIp = InetAddress.getLocalHost().getHostAddress();
            System.out.print("Enter Server IP for RMI [default: " + localIp + "]: ");
            String rmiHost = scanner.nextLine().trim();
            if (rmiHost.isEmpty()) rmiHost = localIp;

            // Critical for LAN multiplayer in RMI
            System.setProperty("java.rmi.server.hostname", rmiHost);

            System.out.print("Enter RMI Port [default: " + NetworkDefaults.DEFAULT_RMI_PORT + "]: ");
            String rmiPortStr = scanner.nextLine().trim();
            int rmiPort = rmiPortStr.isEmpty() ? NetworkDefaults.DEFAULT_RMI_PORT : Integer.parseInt(rmiPortStr);

            System.out.print("Enter Socket Port [default: " + NetworkDefaults.DEFAULT_SOCKET_PORT + "]: ");
            String socketPortStr = scanner.nextLine().trim();
            int socketPort = socketPortStr.isEmpty() ? NetworkDefaults.DEFAULT_SOCKET_PORT : Integer.parseInt(socketPortStr);

            System.out.println("\n[INFO] Starting server on " + rmiHost + "...");

            // --- BOOTSTRAP ---
            GameRegistry.getInstance();
            ControllerManager manager = ControllerManager.getInstance();

            Path logsDir = Paths.get("logs");
            manager.recoverGames(logsDir);
            System.out.println("[INFO] Persistence check completed.");
            System.out.println("----------------------------------------");

            // --- START RMI ---
            NetworkServer rmiServer = NetworkServerFactory.create(NetworkType.RMI);
            Thread rmiThread = new Thread(() -> {
                try {
                    rmiServer.start(rmiPort);
                } catch (Exception e) {
                    System.err.println("[RMI FATAL] Failed to start RMI on port " + rmiPort);
                }
            }, "NetworkServer-RMI");
            rmiThread.setDaemon(true);
            rmiThread.start();

            // --- START SOCKET ---
            NetworkServer socketServer = NetworkServerFactory.create(NetworkType.SOCKET);
            Thread socketThread = new Thread(() -> {
                try {
                    socketServer.start(socketPort);
                } catch (Exception e) {
                    System.err.println("[SOCKET FATAL] Failed to start Socket on port " + socketPort);
                }
            }, "NetworkServer-Socket");
            socketThread.setDaemon(true);
            socketThread.start();

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