package it.polimi.ingsw.am02.client.network.socket;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ClientNetworkDispatcher;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.commands.heartbeat.PingCommand;
import it.polimi.ingsw.am02.common.messages.commands.heartbeat.PongCommand;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.heartbeat.PingEvent;
import it.polimi.ingsw.am02.common.messages.events.heartbeat.PongEvent;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Socket-based proxy on the client side.
 *
 * <p><b>Inbound</b> (server → client): a dedicated reader thread listens on
 * the TCP stream, decodes each newline-delimited JSON message, and dispatches
 * it to the {@link ClientNetworkDispatcher} via the IoC
 * {@link Event#apply(it.polimi.ingsw.am02.common.interfaces.VirtualView)} pattern.
 * {@link PingEvent}s are answered immediately with a {@link PongCommand}.
 *
 * <p><b>Outbound</b> (client → server): {@link Command}s are serialized and
 * written to the socket's output stream on the caller's thread; the stream is
 * backed by an auto-flush {@link PrintWriter} so no explicit flush is needed.
 *
 * <p>Keep-alive uses a bidirectional ping-pong mechanism symmetric to RMI:
 * the server pings the client ({@link PingEvent} → {@link PongCommand}) and
 * the client pings the server ({@link PingCommand} → {@link PongEvent}).
 * Each side resets its liveness timer only on receipt of the corresponding
 * pong, not on generic traffic.
 */
public class SocketServerProxy implements ServerProxy {

    /** How often the ping thread fires, in milliseconds. */
    private static final long PING_INTERVAL_MILLIS = 5_000;

    /** Maximum time allowed since the last {@link PongEvent} before the connection is declared lost. */
    private static final long PING_TIMEOUT_MILLIS = 10_000;

    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView view;
    private final JsonMessageCodec codec = new JsonMessageCodecImpl();

    private ClientController clientController;
    private ClientNetworkDispatcher dispatcher;

    private Socket socket;
    private PrintWriter out;

    private volatile boolean connected = false;
    private volatile boolean attemptingReconnection = false;

    /** Set by {@link #disconnect()} to prevent background threads from reconnecting. */
    private volatile boolean intentionalDisconnect = false;

    private Thread pingThread;

    /**
     * Timestamp of the last received {@link PongEvent}.
     * Reset only on an explicit pong, not on generic server traffic.
     */
    private volatile long lastPongReceivedAt = 0;

    /** Nickname last successfully registered; used to restore session after reconnection. */
    private String activeNickname;

    /** Game ID currently joined; used to rejoin after reconnection. */
    private String activeGameId;

    /**
     * Creates a Socket proxy ready to be wired and connected.
     *
     * @param host       the server hostname or IP address
     * @param port       the server TCP port
     * @param lobbyModel the lobby model that receives pre-game notifications
     * @param view       the client view used to signal connection-state changes
     */
    public SocketServerProxy(String host, int port, LobbyModel lobbyModel, ClientView view) {
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.view = view;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Also registers a callback on the dispatcher so that
     * {@link #activeGameId} is updated whenever a game-started notification arrives.
     */
    @Override
    public void setClientController(ClientController controller) {
        this.clientController = controller;
        this.dispatcher = new ClientNetworkDispatcher(controller, lobbyModel);
        this.dispatcher.setOnGameStarted(id -> this.activeGameId = id);
    }

    /** {@inheritDoc} */
    @Override
    public void connect() throws Exception {
        intentionalDisconnect = false;
        socket = new Socket(host, port);
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;
        lastPongReceivedAt = System.currentTimeMillis();

        Thread readerThread = new Thread(this::listenForEvents, "SocketProxy-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        startPingThread();
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect() {
        intentionalDisconnect = true;
        connected = false;
        activeGameId = null;

        stopPingThread();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConnected() { return connected; }

    // =========================================================
    // READER LOOP
    // =========================================================

    /**
     * Main loop of the reader thread.
     *
     * <p>Dispatches incoming messages at the transport layer:
     * <ul>
     *   <li>{@link PingEvent} — answered immediately with {@link PongCommand};
     *       does <em>not</em> reset the liveness timer</li>
     *   <li>{@link PongEvent} — resets {@link #lastPongReceivedAt}; this is
     *       the server's reply to our own {@link PingCommand}</li>
     *   <li>all other {@link Event}s — forwarded to the dispatcher via IoC</li>
     * </ul>
     *
     * <p>If the loop exits unexpectedly, {@link #handleConnectionLost()} is called.
     */
    private void listenForEvents() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (intentionalDisconnect) break;

                Message msg = codec.decode(line);

                if (msg instanceof PingEvent) {
                    send(new PongCommand());
                    continue;
                }

                if (msg instanceof PongEvent) {
                    lastPongReceivedAt = System.currentTimeMillis();
                    continue;
                }

                if (msg instanceof Event event) {
                    event.apply(dispatcher);
                }
            }
        } catch (IOException ignored) {
            // Socket closed externally.
        } finally {
            if (!intentionalDisconnect) {
                handleConnectionLost();
            }
        }
    }

    // =========================================================
    // PING THREAD
    // =========================================================

    /**
     * Starts a new ping thread, stopping any previously running one first.
     *
     * <p>At every {@link #PING_INTERVAL_MILLIS} tick the thread:
     * <ol>
     *   <li>Sends a {@link PingCommand} to the server — the server replies
     *       with a {@link PongEvent}, which resets {@link #lastPongReceivedAt}.</li>
     *   <li>Checks whether the elapsed time since the last {@link PongEvent}
     *       exceeds {@link #PING_TIMEOUT_MILLIS}; if so, calls
     *       {@link #handleConnectionLost()}.</li>
     * </ol>
     */
    private void startPingThread() {
        stopPingThread();
        pingThread = new Thread(() -> {
            while (connected && !intentionalDisconnect) {
                try {
                    Thread.sleep(PING_INTERVAL_MILLIS);
                    if (!connected || intentionalDisconnect) break;

                    send(new PingCommand());

                    long elapsed = System.currentTimeMillis() - lastPongReceivedAt;
                    if (elapsed > PING_TIMEOUT_MILLIS) {
                        handleConnectionLost();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "SocketProxy-PingThread");
        pingThread.setDaemon(true);
        pingThread.start();
    }

    /**
     * Interrupts and discards the current ping thread, if any.
     */
    private void stopPingThread() {
        if (pingThread != null) {
            pingThread.interrupt();
            pingThread = null;
        }
    }

    // =========================================================
    // CONNECTION LOSS HANDLER
    // =========================================================

    /**
     * Handles an unexpected loss of connectivity.
     *
     * <p>Marks the connection as down, notifies the view, and spawns a
     * reconnection thread that retries every 5 seconds. Once reconnected,
     * replays the login and game-rejoin sequence using {@link #activeNickname}
     * and {@link #activeGameId}.
     *
     * <p>Synchronized to prevent concurrent executions (reader thread vs.
     * ping timeout).
     */
    private synchronized void handleConnectionLost() {
        if (attemptingReconnection || intentionalDisconnect) return;
        connected = false;
        attemptingReconnection = true;

        stopPingThread();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        view.onConnectionLost();

        new Thread(() -> {
            while (!connected && !intentionalDisconnect) {
                try {
                    Thread.sleep(5_000);
                    try { if (socket != null) socket.close(); } catch (IOException ignored) {}

                    connect();

                    if (activeNickname != null && activeGameId != null) {
                        requestReconnect(activeNickname, activeGameId);
                    } else if (activeNickname != null) {
                        requestSetUsername(activeNickname);
                        view.onReturnToLobby();
                    }
                    view.onConnectionRestored();
                    attemptingReconnection = false;
                } catch (Exception ignored) {}
            }
        }, "SocketProxy-ReconnectThread").start();
    }

    // =========================================================
    // OUTBOUND — send helper
    // =========================================================

    /**
     * Serializes and sends a {@link Command} to the server.
     *
     * <p>No-op if the output stream is not yet available or if an intentional
     * disconnect is in progress.
     *
     * @param command the command to send
     */
    private void send(Command command) {
        if (out != null && !intentionalDisconnect) {
            out.println(codec.encode(command));
            out.flush();
        }
    }

    // =========================================================
    // ServerProxy — outbound API
    // =========================================================

    /**
     * {@inheritDoc}
     *
     * <p>Also caches the username in {@link #activeNickname} for use by the
     * reconnection loop.
     */
    @Override
    public void requestSetUsername(String username) {
        this.activeNickname = username;
        send(new SetUsernameCommand(username));
    }

    /** {@inheritDoc} */
    @Override
    public void requestCreateLobby(int numPlayers) {
        send(new CreateLobbyCommand(numPlayers));
    }

    /** {@inheritDoc} */
    @Override
    public void requestJoinLobby(String lobbyID) {
        send(new JoinLobbyCommand(lobbyID));
    }

    /** {@inheritDoc} */
    @Override
    public void requestSelectTotem(Totem color) {
        send(new SelectTotemCommand(color));
    }

    /** {@inheritDoc} */
    @Override
    public void requestLeaveLobby() {
        send(new LeaveLobbyCommand(activeNickname != null ? activeNickname : ""));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Also updates {@link #activeNickname}, {@link #activeGameId}, and the
     * dispatcher's internal state so that the reconnection loop can replay
     * this call after a network failure.
     */
    @Override
    public void requestReconnect(String nickname, String gameId) {
        this.activeNickname = nickname;
        this.activeGameId = gameId;

        if (this.dispatcher != null) {
            this.dispatcher.updateActiveNickname(nickname);
            this.dispatcher.updateActiveGameId(gameId);
        }

        send(new ReconnectCommand(nickname, gameId));
    }

    /** {@inheritDoc} */
    @Override
    public void moveTotem(char tileID) {
        send(new MoveTotemCommand("", tileID));
    }

    /** {@inheritDoc} */
    @Override
    public void resolveActions(List<String> ids) {
        send(new ResolveActionsCommand("", ids));
    }
}