package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.GameEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.GameStartedEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.LobbyEvent;
import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerFactory;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClientRemote {

    private final String host;
    private final int port;

    private final LobbyModel lobbyModel;
    private final ClientView clientView; // Added to fix the Factory signature
    private GameModel gameModel; // Lazily initialized

    private RmiServerRemote serverStub;
    private boolean connected = false;

    public RmiServerProxy(String host, int port, LobbyModel lobbyModel, ClientView clientView) throws RemoteException {
        super();
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.clientView = clientView; // Store the view reference
    }

    @Override
    public void connect() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        RmiServerFactory factory = (RmiServerFactory) registry.lookup("AM02-GameServer");
        this.serverStub = factory.registerClient(this);
        this.connected = true;
    }

    @Override
    public void disconnect() {
        this.connected = false;
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

// ... inside RmiServerProxy.java ...

    private String reconnectedNickname = null;

    @Override
    public void ping() throws RemoteException {
        // Do nothing. We just need to receive the call to prove we are alive.
    }

    @Override
    public void requestReconnect(String gameId, String nickname) {
        this.reconnectedNickname = nickname; // Save it for lazy initialization
        try { serverStub.requestReconnect(gameId, nickname); } catch (RemoteException e) { handleNetworkError(e); }
    }

    // Replace the existing notifyEvent with this one:
    @Override
    public void notifyEvent(Event event) throws RemoteException {
        if (event instanceof LobbyEvent lobbyEvent) {
            lobbyModel.apply(lobbyEvent);
            if (event instanceof GameStartedEvent && gameModel == null) {
                initGameModel(lobbyModel.getMyNickname());
            }
        } else if (event instanceof GameEvent gameEvent) {
            if (gameModel == null) {
                // RECONNECTION FIRED! We missed the GameStartedEvent, initialize now!
                String nick = lobbyModel.getMyNickname() != null ? lobbyModel.getMyNickname() : reconnectedNickname;
                initGameModel(nick);
                System.out.println("[RMI Proxy] GameModel RECOVERED via GameEvent for: " + nick);
            }
            gameModel.apply(gameEvent);
        }
    }

    private void initGameModel(String nickname) {
        this.gameModel = new GameModel(nickname);
        if (this.clientView instanceof it.polimi.ingsw.am02.client.view.tui.TuiView tui) {
            tui.onGameModelCreated(this.gameModel);
        }
    }

    // --- SERVER PROXY (Outbound calls to Server) ---
    @Override
    public void requestSetUsername(String username) {
        try { serverStub.requestSetUsername(username); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void requestCreateLobby(int numPlayers) {
        try { serverStub.requestCreateLobby(numPlayers); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void requestJoinLobby(String lobbyID) {
        try { serverStub.requestJoinLobby(lobbyID); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void requestSelectTotem(Totem color) {
        try { serverStub.requestSelectTotem(color); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void requestStartGame() {
        try { serverStub.requestStartGame(); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void requestLeaveLobby() {
        try { serverStub.requestLeaveLobby(); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void moveTotem(char tileID) {
        try { serverStub.moveTotem(tileID); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void resolveActions(List<String> selectedIDs) {
        try { serverStub.resolveActions(selectedIDs); } catch (RemoteException e) { handleNetworkError(e); }
    }

    private void handleNetworkError(RemoteException e) {
        this.connected = false;
        System.err.println("[RMI Proxy] Network disconnected: " + e.getMessage());
    }

    public GameModel getGameModel() {
        return gameModel;
    }

}