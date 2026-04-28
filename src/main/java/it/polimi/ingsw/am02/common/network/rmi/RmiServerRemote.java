package it.polimi.ingsw.am02.common.network.rmi;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RmiServerRemote extends Remote {


    // --- Lobby ---
    void requestSetUsername(String username) throws RemoteException;
    void requestCreateLobby(int numPlayers) throws RemoteException;
    void requestJoinLobby(String lobbyID) throws RemoteException;
    void requestSelectTotem(Totem color) throws RemoteException;
    void requestStartGame() throws RemoteException;
    void requestLeaveLobby() throws RemoteException;

    // --- Game ---
    void moveTotem(String nickname, char tileID) throws RemoteException;
    void resolveActions(String nickname, List<String> selectedIDs) throws RemoteException;
}