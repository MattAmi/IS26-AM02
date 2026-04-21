package it.polimi.ingsw.am02.server.network.socket;

import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;
import it.polimi.ingsw.am02.server.network.NetworkServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer implements NetworkServer {

    private final JsonMessageCodec codec = new JsonMessageCodecImpl();
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    @Override
    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("[SocketServer] In ascolto sulla porta " + port);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SocketServer] New Connection: "
                        + clientSocket.getInetAddress());
                SocketClientHandler handler = new SocketClientHandler(clientSocket, codec);
                new Thread(handler::listen).start();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[SocketServer] Error: " + e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}