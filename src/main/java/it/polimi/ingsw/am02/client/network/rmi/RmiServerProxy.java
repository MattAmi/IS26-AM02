package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;
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
    private RmiServerRemote serverStub;
    private boolean connected = false;

    public RmiServerProxy(String host, int port) throws RemoteException {
        super();
        this.host = host;
        this.port = port;
    }

    @Override
    public void connect() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        RmiServerFactory factory = (RmiServerFactory) registry.lookup("AM02-GameServer");
        this.serverStub = factory.registerClient(this);
        this.connected = true;
        System.out.println("Connected to RMI Server!");
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
        if (event instanceof UsernameResultEvent e) {
            System.out.println("\n[SERVER] Username Result: " + (e.isValid() ? "ACCEPTED" : "REJECTED"));
        }

        else if (event instanceof UpdatedLobbiesEvent e) {
            System.out.println("\n[SERVER] Updated Lobby List:");
            if (e.lobbies().isEmpty()) {
                System.out.println("   (No lobbies available)");
            } else {
                e.lobbies().forEach(l ->
                        System.out.println("   > ID: " + l.lobbyId() + " | Players: " + l.currentPlayers())
                );
            }
        }

        else if (event instanceof UpdatedLobbyEvent e) {
            System.out.println("\n[SERVER] Current Lobby Updated!");
            System.out.println("   LOBBY ID: " + e.lobby().lobbyId());
        }

        else {
            System.out.println("\n[SERVER] Event received: " + event.getClass().getSimpleName());
        }

        System.out.print("\nWhat do you want to do?\n1. Create Lobby\n2. Join Lobby\n3. Exit\n> ");
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
        System.err.println("RMI Network Error: " + e.getMessage());
        this.connected = false;
    }
}