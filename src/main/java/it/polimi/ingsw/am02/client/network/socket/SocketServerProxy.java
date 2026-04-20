package it.polimi.ingsw.am02.client.network.socket;

import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualServer;

import java.util.List;

public class SocketServerProxy implements ServerProxy {
    @Override
    public void connect() throws Exception {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void requestSetUsername(String username) {

    }

    @Override
    public void requestCreateLobby(int numPlayers) {

    }

    @Override
    public void requestJoinLobby(String lobbyID) {

    }

    @Override
    public void requestSelectTotem(Totem color) {

    }

    @Override
    public void requestStartGame() {

    }

    @Override
    public void requestLeaveLobby() {

    }

    @Override
    public void moveTotem(String nickname, char tileID) {

    }

    @Override
    public void resolveActions(String nickname, List<String> selectedIDs) {

    }
}
