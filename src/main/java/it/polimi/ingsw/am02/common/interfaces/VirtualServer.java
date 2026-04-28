package it.polimi.ingsw.am02.common.interfaces;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import java.util.List;


public interface VirtualServer {

    // Lobby
    void requestSetUsername(String username);
    void requestCreateLobby(int numPlayers);
    void requestJoinLobby(String lobbyID);
    void requestSelectTotem(Totem color);
    void requestStartGame();
    void requestLeaveLobby();
    // Reconnection
    void requestReconnect(String gameId, String nickname);
    // Game
    void moveTotem(char tileID);
    void resolveActions(List<String> selectedIDs);

}
