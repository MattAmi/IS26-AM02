package it.polimi.ingsw.am02.common.interfaces;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import java.util.List;

/**
 * Client-side abstraction of the server.
 * Implemented by {@link it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy}
 * and {@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy}.
 *
 * <p>The client controller calls these methods to send actions to the server;
 * the proxy handles serialization and transport transparently.
 */
public interface VirtualServer {
    // Lobby
    /** Requests that the server assign the given username to this client. */
    void requestSetUsername(String username);

    /**
     * Requests creation of a new lobby.
     *
     * @param numPlayers the desired player capacity (2–5)
     */
    void requestCreateLobby(int numPlayers);

    /**
     * Requests to join an existing lobby.
     *
     * @param lobbyID the target lobby identifier
     */
    void requestJoinLobby(String lobbyID);

    /**
     * Requests totem selection within the current lobby.
     *
     * @param color the desired totem color
     */
    void requestSelectTotem(Totem color);

    /** Requests to leave the current lobby and return to the pre-lobby state. */
    void requestLeaveLobby();

    // Reconnection

    /**
     * Requests reconnection to an ongoing game.
     *
     * @param nickname the nickname used in the original session
     * @param gameId   the identifier of the game to reconnect to
     */
    void requestReconnect(String nickname, String gameId);

    // Game

    /**
     * Sends a totem-placement or totem-return action.
     *
     * @param tileID the single-character identifier of the target tile
     *               (use {@code 'T'} to return the totem to the turn-order tile)
     */
    void moveTotem(char tileID);

    /**
     * Sends a card-selection action.
     *
     * @param selectedIDs the list of card IDs the player wishes to acquire
     */
    void resolveActions(List<String> selectedIDs);
}
