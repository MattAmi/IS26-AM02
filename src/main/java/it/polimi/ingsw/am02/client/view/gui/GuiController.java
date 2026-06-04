package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;

/**
 * Controller for the Graphical User Interface (GUI).
 * This class handles the communication between the GUI views and the server proxy,
 * managing the lobby and game phases.
 */
public class GuiController extends ClientController {

    private NetworkType networkType;
    private String host;
    private int port;

    /**
     * Constructs a new GuiController.
     *
     * @param proxy      The server proxy to use for communication.
     * @param lobbyModel The model containing lobby-related data.
     * @param view       The client view associated with this controller.
     */
    public GuiController(ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        super(proxy, lobbyModel, view);
    }

    /**
     * Sets the connection configuration for the server.
     *
     * @param networkType The type of network connection (RMI or Socket).
     * @param host        The server hostname or IP address.
     * @param port        The server port.
     */
    public void setConnectionConfig(NetworkType networkType, String host, int port) {
        this.networkType = networkType;
        this.host = host;
        this.port = port;
    }

    /**
     * Creates a new server proxy using the stored connection configuration.
     *
     * @return A new ServerProxy instance.
     * @throws Exception If an error occurs during proxy creation.
     */
    @Override
    protected ServerProxy createNewProxy() throws Exception {
        return ServerProxyFactory.create(networkType, host, port, lobbyModel, view);
    }

    /**
     * Retrieves the list of available lobbies from the lobby model.
     *
     * @return A list of LobbyInfo objects representing available lobbies.
     */
    public List<LobbyInfo> getAvailableLobbies() {
        return lobbyModel.getAvailableLobbies();
    }

    /**
     * Requests to set the user's nickname.
     *
     * @param nickname The desired nickname.
     */
    public void requestSetUsername(String nickname) { handleSetNickname(nickname); }

    /**
     * Requests the creation of a new lobby.
     *
     * @param size The expected number of players in the lobby.
     */
    public void requestCreateLobby(int size) { handleCreateLobby(size); }

    /**
     * Requests to join a specific lobby by its ID.
     *
     * @param lobbyId The ID of the lobby to join.
     */
    public void requestJoinLobby(String lobbyId) {
        List<LobbyInfo> lobbies = lobbyModel.getAvailableLobbies();
        for (LobbyInfo l : lobbies) {
            if (l.lobbyId().equals(lobbyId)) {
                handleJoinLobby(lobbies.indexOf(l));
                return;
            }
        }
    }

    /**
     * Requests to reconnect to an ongoing game.
     *
     * @param nick The user's nickname.
     * @param gId  The game ID to reconnect to.
     */
    public void requestReconnect(String nick, String gId) { handleReconnect(nick, gId); }

    /**
     * Requests to select a totem.
     *
     * @param t The totem color to select.
     */
    public void requestSelectTotem(Totem t) { handleSelectTotem(t); }

    /**
     * Requests to leave the current lobby.
     * If not in a lobby, performs a return to the lobby screen.
     */
    public void requestLeaveLobby() {
        if (lobbyModel.getCurrentLobby() != null) proxy.requestLeaveLobby();
        else performReturnToLobby();
    }

    /**
     * Sends a request to move the totem to a specific tile.
     *
     * @param t The tile ID to move the totem to.
     */
    public void moveTotem(char t) { handleMoveTotem(t); }

    /**
     * Sends a request to resolve actions with a list of selected cards.
     *
     * @param ids The list of card IDs to take.
     */
    public void resolveActions(List<String> ids) { handleResolveActions(ids); }

    /**
     * Requests to return to the lobby screen.
     */
    public void requestReturnToLobby() { performReturnToLobby(); }
}