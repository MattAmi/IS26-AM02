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

    // Game
    void moveTotem(String nickname, char tileID);
    void resolveActions(String nickname, List<String> selectedIDs);

}
