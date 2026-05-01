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

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SERVER IN ASCOLTO ===");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");

        // 1. Inizializzazione Dominio
        GameRegistry.getInstance();
        ControllerManager manager = ControllerManager.getInstance();

        // 2. RECUPERO PERSISTENZA
        Path logsDir = Paths.get("logs");
        manager.recoverGames(logsDir);

        // 3. Avvio RMI Server sul proprio thread (start() è bloccante)
        NetworkServer rmiServer = NetworkServerFactory.create(NetworkType.RMI);
        Thread rmiThread = new Thread(() -> rmiServer.start(RMI_PORT), "RmiServer-Thread");
        rmiThread.setDaemon(true);
        rmiThread.start();

        // 4. Avvio Socket Server sul proprio thread (start() è bloccante)
        NetworkServer socketServer = NetworkServerFactory.create(NetworkType.SOCKET);
        Thread socketThread = new Thread(() -> socketServer.start(SOCKET_PORT), "SocketServer-Thread");
        socketThread.setDaemon(true);
        socketThread.start();

        System.out.println("RMI    server avviato sulla porta " + RMI_PORT);
        System.out.println("Socket server avviato sulla porta " + SOCKET_PORT);

        // Tieni vivo il processo principale
        Thread.currentThread().join();
    }
}