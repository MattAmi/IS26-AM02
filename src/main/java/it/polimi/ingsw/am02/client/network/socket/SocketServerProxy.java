package it.polimi.ingsw.am02.client.network.socket;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.error.ErrorEvent;
import it.polimi.ingsw.am02.common.messages.events.game.GameEvent;
import it.polimi.ingsw.am02.common.messages.events.game.PingEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.GameStartedEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.LobbyEvent;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SocketServerProxy implements ServerProxy {

    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView view;
    private GameModel gameModel; // null fino a GameStartedEvent

    private final JsonMessageCodec codec = new JsonMessageCodecImpl();
    private String myNickname;
    private Socket socket;
    private PrintWriter out;
    private boolean connected = false;

    public SocketServerProxy(String host, int port, LobbyModel lobbyModel, ClientView view) {
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.view = view;
    }

    @Override
    public void connect() throws Exception {
        socket = new Socket(host, port);
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
        );
        connected = true;
        new Thread(this::listenForEvents).start();
    }

    private void listenForEvents() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = codec.decode(line);
                if (msg instanceof PingEvent) {
                    send(new PongCommand());
                } else if (msg instanceof Event event) {
                    route(event);
                }
            }
        } catch (IOException e) {
            System.out.println("[SocketServerProxy] Connection lost.");
        } finally {
            connected = false;
        }
    }

    private void route(Event event) {
        switch (event) {
            case GameStartedEvent e -> {
                this.gameModel = new GameModel(lobbyModel.getMyNickname());
                this.gameModel.addObserver(view);
                gameModel.apply(e);
            }
            case LobbyEvent e -> lobbyModel.apply(e);
            case GameEvent e -> {
                if (gameModel != null) gameModel.apply(e);
            }
            case ErrorEvent e -> {
                if (gameModel != null) gameModel.apply(e);
                else lobbyModel.apply(e);
            }
            default -> throw new IllegalStateException("Unexpected value: " + event);
        }
    }

    private void send(Command command) {
        if (out != null) out.println(codec.encode(command));
    }

    @Override
    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public boolean isConnected() { return connected; }

    @Override public void requestSetUsername(String username)          { send(new SetUsernameCommand(username)); }
    @Override public void requestCreateLobby(int numPlayers)          { send(new CreateLobbyCommand(numPlayers)); }
    @Override public void requestJoinLobby(String lobbyID)            { send(new JoinLobbyCommand(lobbyID)); }
    @Override public void requestSelectTotem(Totem color)             { send(new SelectTotemCommand(color)); }
    @Override public void requestStartGame()                          { send(new StartGameCommand()); }
    @Override public void requestLeaveLobby()                         { send(new LeaveLobbyCommand(myNickname)); }

    @Override
    public void requestReconnect(String nickname, String gameId) {
        send(new ReconnectCommand(nickname, gameId));
    }

    @Override public void moveTotem(char tileID)     { send(new MoveTotemCommand("not_set", tileID)); }
    @Override public void resolveActions(List<String> ids) { send(new ResolveActionsCommand("not_set", ids)); }
}