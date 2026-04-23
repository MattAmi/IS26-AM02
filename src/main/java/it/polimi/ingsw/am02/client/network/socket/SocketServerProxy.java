package it.polimi.ingsw.am02.client.network.socket;

import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodec;
import it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SocketServerProxy implements ServerProxy {

    private final String host;
    private final int port;
    private final VirtualView clientModel;
    private final JsonMessageCodec codec = new JsonMessageCodecImpl();
    private String myNickname;

    private Socket socket;
    private PrintWriter out;
    private boolean connected = false;

    public SocketServerProxy(String host, int port, VirtualView clientModel) {
        this.host = host;
        this.port = port;
        this.clientModel = clientModel;
    }

    @Override
    public void connect() throws Exception {
        socket = new Socket(host, port);
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
        );
        connected = true;
        // Thread separato che ascolta gli Event in arrivo dal server
        new Thread(this::listenForEvents).start();
    }

    private void listenForEvents() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = codec.decode(line);
                if (msg instanceof Event event) {
                    clientModel.notify(event);
                    if (event instanceof UsernameResultEvent e && e.isValid()) {
                        this.myNickname = e.username();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[SocketServerProxy] Connessione persa.");
        } finally {
            connected = false;
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

    // --- VirtualServer ---
    @Override public void requestSetUsername(String username) { send(new SetUsernameCommand(username)); }
    @Override public void requestCreateLobby(int numPlayers) { send(new CreateLobbyCommand(numPlayers)); }
    @Override public void requestJoinLobby(String lobbyID) { send(new JoinLobbyCommand(lobbyID)); }
    @Override public void requestSelectTotem(Totem color) { send(new SelectTotemCommand(color)); }
    @Override public void requestStartGame() { send(new StartGameCommand()); }
    @Override public void requestLeaveLobby() { send(new LeaveLobbyCommand(myNickname)); }
    @Override public void moveTotem(String nickname, char tileID) { send(new MoveTotemCommand(nickname, tileID)); }
    @Override public void resolveActions(String nickname, List<String> selectedIDs) { send(new ResolveActionsCommand(nickname, selectedIDs));}
}