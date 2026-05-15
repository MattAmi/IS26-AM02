package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;

public class GuiController extends ClientController {

    public GuiController(ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        super(proxy, lobbyModel, view);
    }

    public List<LobbyInfo> getAvailableLobbies() {
        return lobbyModel.getAvailableLobbies();
    }

    public void requestSetUsername(String nickname) { handleSetNickname(nickname); }

    public void requestCreateLobby(int size) { handleCreateLobby(size); }

    public void requestJoinLobby(String lobbyId) {
        List<LobbyInfo> lobbies = lobbyModel.getAvailableLobbies();
        for (LobbyInfo l : lobbies) {
            if (l.lobbyId().equals(lobbyId)) {
                handleJoinLobby(lobbies.indexOf(l));
                return;
            }
        }
    }

    public void requestReconnect(String nick, String gId) { handleReconnect(nick, gId); }

    public void requestSelectTotem(Totem t) { handleSelectTotem(t); }

    public void requestLeaveLobby() {
        if (lobbyModel.getCurrentLobby() != null) proxy.requestLeaveLobby();
        else performReturnToLobby();
    }

    public void moveTotem(char t) { handleMoveTotem(t); }

    public void resolveActions(List<String> ids) { handleResolveActions(ids); }

    public void requestReturnToLobby() { performReturnToLobby(); }
}