package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.GameEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.GameStartedEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.LobbyEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;
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
    private final ClientView clientView;
    private GameModel gameModel;
    private RmiServerRemote serverStub;
    private boolean connected = false;

    // Cache per gestire la riconnessione
    private String activeNickname = null;
    private String activeGameId = null;

    public RmiServerProxy(String host, int port, LobbyModel lobbyModel, ClientView clientView) throws RemoteException {
        super();
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.clientView = clientView;
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
        try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) {}
    }

    @Override public boolean isConnected() { return connected; }
    @Override public void ping() throws RemoteException {}

    @Override
    public void notifyEvent(Event event) throws RemoteException {
        // Memorizziamo nick e gameId se passano dalla lobby
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.activeNickname = e.username();
        }
        if (event instanceof GameStartedEvent e) {
            this.activeGameId = e.gameID();
        }

        if (event instanceof LobbyEvent lobbyEvent) {
            lobbyModel.apply(lobbyEvent);
            if (event instanceof GameStartedEvent && gameModel == null) {
                initGameModel(this.activeNickname);
            }
        } else if (event instanceof GameEvent gameEvent) {
            if (gameModel == null) {
                // Se siamo in riconnessione, lobbyModel.getMyNickname() è null.
                // Usiamo activeNickname che abbiamo salvato nel comando reconnect.
                initGameModel(this.activeNickname);
                System.out.println("[RMI Proxy] GameModel inizializzato durante la riconnessione per: " + this.activeNickname);
            }
            gameModel.apply(gameEvent);
        }
    }

    private void initGameModel(String nickname) {
        this.gameModel = new GameModel(nickname);

        // Colleghiamo la TUI
        if (clientView instanceof TuiView tui) {
            tui.onGameModelCreated(this.gameModel);
        }
        this.clientView.setGameModel(this.gameModel);

        // Se conosciamo il gameId (da reconnect o da cache), lo iniettiamo nel model
        // così la TUI non stampa più "Game: null"
        if (this.activeGameId != null) {
            this.gameModel.apply(new GameStartedEvent(this.activeGameId));
        }
    }

    @Override
    public void requestReconnect(String nickname, String gameId) {
        // SALVATAGGIO VITALE: salviamo i dati PRIMA di mandare il comando
        this.activeNickname = nickname;
        this.activeGameId = gameId;
        try {
            serverStub.requestReconnect(nickname, gameId);
        } catch (RemoteException e) {
            this.connected = false;
        }
    }

    // --- Altri metodi Outbound ---
    @Override public void requestSetUsername(String u) { try { serverStub.requestSetUsername(u); } catch (RemoteException e) { connected = false; } }
    @Override public void requestCreateLobby(int n) { try { serverStub.requestCreateLobby(n); } catch (RemoteException e) { connected = false; } }
    @Override public void requestJoinLobby(String id) { try { serverStub.requestJoinLobby(id); } catch (RemoteException e) { connected = false; } }
    @Override public void requestSelectTotem(Totem t) { try { serverStub.requestSelectTotem(t); } catch (RemoteException e) { connected = false; } }
    @Override public void requestStartGame() {}
    @Override public void requestLeaveLobby() { try { serverStub.requestLeaveLobby(); } catch (RemoteException e) { connected = false; } }
    @Override public void moveTotem(char t) { try { serverStub.moveTotem(t); } catch (RemoteException e) { connected = false; } }
    @Override public void resolveActions(List<String> ids) { try { serverStub.resolveActions(ids); } catch (RemoteException e) { connected = false; } }
}