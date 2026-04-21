package it.polimi.ingsw.am02.server.network.socket;

import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.network.ClientHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketClientHandler implements ClientHandler {

    private final Socket socket;
    private final JsonMessageCodec codec;
    private final ControllerManager manager;
    private final PrintWriter out;
    private String myNickname;

    public SocketClientHandler(Socket socket, JsonMessageCodec codec) throws IOException {
        this.socket = socket;
        this.codec = codec;
        this.manager = ControllerManager.getInstance();
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
        );
    }

    //Runs on a dedicated thread, reads messages until the client is connected
    public void listen() {
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
            System.out.println("[SocketClientHandler] Client Disconnected: "
                    + (myNickname != null ? myNickname : socket.getInetAddress()));
        } finally {
            disconnect();
        }
    }

    private void dispatch(Command cmd) {
        switch (cmd) {
            case SetUsernameCommand c    -> {
                manager.requestSetUsername(c.username(), this);
            }
            case CreateLobbyCommand c   -> {
                if (myNickname == null) return;
                manager.createLobby(c.numPlayers(), myNickname, this);
            }
            case JoinLobbyCommand c     -> {
                if (myNickname == null) return;
                manager.joinLobby(c.lobbyID(), myNickname, this);
            }
            case SelectTotemCommand c   -> {
                if (myNickname == null) return;
                manager.selectTotem(myNickname, c.color());
            }
            case StartGameCommand c     -> {
                if (myNickname == null) return;
                manager.routeGameCommand(myNickname, c);
            }
            case LeaveLobbyCommand c    -> {
                if (myNickname == null) return;
                manager.leaveLobby(myNickname);
            }
            case MoveTotemCommand c     -> {
                if (myNickname == null) return;
                manager.routeGameCommand(myNickname, c);
            }
            case ResolveActionsCommand c -> {
                if (myNickname == null) return;
                manager.routeGameCommand(myNickname, c);
            }
        }
    }

    @Override
    public void notify(Event event) {
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.myNickname = e.username();
        }
        out.println(codec.encode(event));
    }

    @Override
    public void disconnect() {
        try { socket.close(); } catch (IOException ignored) {}
        if (myNickname != null) {
            manager.handleDisconnection(myNickname);
        }
    }
}
