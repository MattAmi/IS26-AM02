package it.polimi.ingsw.am02.common.interfaces;

import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;

/**
 * Granular interface through which network handlers communicate with the server controller layer.
 *
 * <p>This is the server-side counterpart of {@link VirtualServer}: just as {@code VirtualServer}
 * exposes granular methods that the client calls on its proxy of the server, this interface exposes
 * granular methods that network handlers ({@code SocketClientHandler}, {@code RmiClientHandler})
 * call on {@code ControllerManager}.
 *
 * <p>Each method corresponds to a single client-initiated action and carries an explicit
 * {@code clientId} parameter (a UUID string assigned at connection time). Network handlers
 * are responsible for injecting the correct {@code clientId} before delegating here;
 * {@code ControllerManager} uses it to look up the associated player, lobby, and game.
 *
 * <p>This abstraction achieves the "collapse upward" principle recommended for mixed
 * Socket/RMI architectures: both transport implementations converge on the same
 * high-level method calls, keeping {@code ControllerManager} and {@code GameController}
 * unaware of the underlying transport.
 */
public interface VirtualControllerManager {

    // =========================================================
    // Connection lifecycle
    // =========================================================

    /**
     * Called when a new client establishes a connection.
     * Registers the client's {@link VirtualView} and returns the assigned {@code clientId}.
     *
     * @param view the server-side proxy for sending notifications to this client
     * @return the unique client identifier assigned to this connection
     */
    String handleClientConnected(VirtualView view);

    /**
     * Called when a client's connection is lost or explicitly closed.
     * Handles lobby removal or in-game disconnection as appropriate.
     *
     * @param clientId the identifier of the disconnected client
     */
    void handleDisconnection(String clientId);

    // =========================================================
    // Lobby actions
    // =========================================================

    /**
     * Requests that the given nickname be assigned to the client in their current lobby.
     *
     * @param clientId the requesting client
     * @param username the desired nickname
     */
    void requestSetUsername(String clientId, String username);

    /**
     * Requests creation of a new lobby with the specified player capacity.
     *
     * @param clientId   the requesting client, who becomes the lobby creator
     * @param numPlayers the total number of players the lobby should wait for (2–5)
     */
    void requestCreateLobby(String clientId, int numPlayers);

    /**
     * Requests that the client join an existing lobby identified by {@code lobbyId}.
     *
     * @param clientId the requesting client
     * @param lobbyId  the target lobby identifier
     */
    void requestJoinLobby(String clientId, String lobbyId);

    /**
     * Requests totem selection for the client within their current lobby.
     *
     * @param clientId the requesting client
     * @param totem    the chosen totem color
     */
    void requestSelectTotem(String clientId, Totem totem);

    /**
     * Requests that the client leave their current lobby.
     * The client is returned to the pre-lobby state and notified of available lobbies.
     *
     * @param clientId the requesting client
     */
    void requestLeaveLobby(String clientId);

    // =========================================================
    // Reconnection
    // =========================================================

    /**
     * Requests reconnection of a previously disconnected player to an ongoing game.
     * The client must supply the nickname they used in the original session and the game identifier.
     *
     * @param clientId the new connection's client identifier
     * @param view     the new {@link VirtualView} for sending notifications to this client
     * @param nickname the nickname of the player attempting to reconnect
     * @param gameId   the identifier of the game to reconnect to
     */
    void requestReconnect(String clientId, VirtualView view, String nickname, String gameId);

    // =========================================================
    // In-game actions
    // =========================================================

    /**
     * Requests that the current player move their totem to the specified offer tile.
     *
     * @param clientId the requesting client
     * @param tileId   the letter identifier of the target offer tile
     */
    void requestMoveTotem(String clientId, char tileId);

    /**
     * Requests resolution of the current player's actions with the given set of selected card IDs.
     *
     * @param clientId    the requesting client
     * @param selectedIds the list of card identifiers the player wishes to take
     */
    void requestResolveActions(String clientId, List<String> selectedIds);
}