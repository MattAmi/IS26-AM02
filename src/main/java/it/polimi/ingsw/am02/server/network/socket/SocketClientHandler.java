package it.polimi.ingsw.am02.server.network.socket;

import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.PingEvent;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.network.ClientHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class SocketClientHandler implements ClientHandler {

    private final Socket socket;
    private final JsonMessageCodec codec;
    private final ControllerManager manager;
    private final PrintWriter out;
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean pongReceived = false;
    private String clientId;
    private volatile long lastPongReceivedAt = System.currentTimeMillis();

    private static final int PING_INTERVAL_SECONDS = 5;
    private static final int PING_TIMEOUT_SECONDS = 10;

    public SocketClientHandler(Socket socket, JsonMessageCodec codec) throws IOException {
        this.socket = socket;
        this.codec = codec;
        this.manager = ControllerManager.getInstance();
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
        );
        this.clientId = manager.handleClientConnected(this);
        this.lastPongReceivedAt = System.currentTimeMillis();
    }

    public void listen() {
        // Reads from the queue and writes on the socket
        Thread writerThread = new Thread(this::writerLoop);
        writerThread.setDaemon(true);
        writerThread.start();

        startPingTimer();
        // Reads from socket
        readerLoop();
    }

    private void startPingTimer() {
        // Invio periodico del ping
        pingScheduler.scheduleAtFixedRate(() -> {
            eventQueue.offer(new PingEvent());
        }, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // Check timeout separato e più lento (non accoppiato all'invio)
        pingScheduler.scheduleAtFixedRate(() -> {
            long elapsed = System.currentTimeMillis() - lastPongReceivedAt;
            if (elapsed > (PING_TIMEOUT_SECONDS + PING_INTERVAL_SECONDS) * 1000L) {
                System.out.println("[SocketClientHandler] Timeout PING per clientId: " + clientId);
                disconnect();
            }
        }, PING_TIMEOUT_SECONDS, PING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void readerLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = codec.decode(line);
                if (msg instanceof Command cmd) {
                    dispatch(cmd);
                }
            }
        } catch (IOException e) {
            System.out.println("[SocketClientHandler] Client Disconnected: " + clientId);
        } finally {
            disconnect();
        }
    }

    private void writerLoop() {
        try {
            while (!socket.isClosed()) {
                Event event = eventQueue.take(); // blocked until further notice
                out.println(codec.encode(event));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void dispatch(Command cmd) {
        switch (cmd) {
            case PongCommand c -> {
                pongReceived = true;
                lastPongReceivedAt = System.currentTimeMillis();
            }
            case SetUsernameCommand c    -> manager.requestSetUsernameInLobby(clientId, c.username());
            case CreateLobbyCommand c    -> manager.createLobby(clientId, c.numPlayers());
            case JoinLobbyCommand c      -> manager.joinLobby(clientId, c.lobbyID());
            case SelectTotemCommand c    -> manager.selectTotem(clientId, c.color());
            case StartGameCommand c      -> manager.routeGameCommand(clientId, c);
            case LeaveLobbyCommand c     -> manager.leaveLobby(clientId);
            case MoveTotemCommand c      -> manager.routeGameCommand(clientId, c);
            case ResolveActionsCommand c -> manager.routeGameCommand(clientId, c);
            case ReconnectCommand c      -> manager.handleReconnectRequest(clientId, this, c);
        }
    }


    @Override
    public void notify(Event event) {
        eventQueue.offer(event);
    }

    @Override
    public synchronized void disconnect() {
        if (clientId == null) return;  // già disconnesso, esce subito
        String id = clientId;
        clientId = null;               // nullifica PRIMA di chiamare il manager
        pingScheduler.shutdownNow();
        try { socket.close(); } catch (IOException ignored) {}
        manager.handleDisconnection(id);
    }
}