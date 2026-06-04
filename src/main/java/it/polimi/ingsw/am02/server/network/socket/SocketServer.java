package it.polimi.ingsw.am02.server.network.socket;

import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;
import it.polimi.ingsw.am02.server.network.NetworkServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * {@link NetworkServer} implementation for the Socket transport.
 *
 * <p>Listens for incoming TCP connections on the configured port. For each
 * accepted connection a {@link SocketClientHandler} is created and started on
 * a dedicated daemon thread, which then drives the handler's read/write loops
 * for the lifetime of that connection.
 */
public class SocketServer implements NetworkServer {

    private final JsonMessageCodec codec = new JsonMessageCodecImpl();
    private ServerSocket serverSocket;
    private volatile boolean running = false;


    /**
     * Starts the server and blocks in the accept loop until {@link #stop()} is called.
     *
     * <p>Each accepted connection is handed off to a new {@link SocketClientHandler}
     * running on its own thread; this method therefore must be called from a
     * dedicated server thread and not from the application main thread.
     *
     * @param port the TCP port to listen on
     */
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

    /**
     * Stops the accept loop and closes the {@link ServerSocket}, causing
     * {@link #start(int)} to return.
     */
    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}