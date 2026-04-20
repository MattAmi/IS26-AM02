package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.events.Event;
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
    private final VirtualView clientModel;
    private RmiServerRemote serverStub;
    private boolean connected = false;

    public RmiServerProxy(String host, int port, VirtualView clientModel) throws RemoteException {
        super();
        this.host = host;
        this.port = port;
        this.clientModel = clientModel;
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

    @Override
    public void notifyEvent(Event event) throws RemoteException {
        clientModel.notify(event);
    }

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
    public void moveTotem(String nickname, char tileID) {
        try { serverStub.moveTotem(nickname, tileID); } catch (RemoteException e) { handleNetworkError(e); }
    }

    @Override
    public void resolveActions(String nickname, List<String> selectedIDs) {
        try { serverStub.resolveActions(nickname, selectedIDs); } catch (RemoteException e) { handleNetworkError(e); }
    }

    private void handleNetworkError(RemoteException e) {
        this.connected = false;
    }
}