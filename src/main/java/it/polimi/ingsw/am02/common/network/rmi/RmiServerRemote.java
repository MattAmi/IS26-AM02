package it.polimi.ingsw.am02.common.network.rmi;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI remote interface exposing client-to-server game and lobby operations.
 *
 * <p>Each method corresponds to a player action. On the server side these calls
 * land on the RMI thread pool and are forwarded to
 * {@link it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager}.
 * The symmetrical inversion-of-control pattern is implemented on the Socket
 * transport via {@link it.polimi.ingsw.am02.common.messages.commands.Command#apply}.
 */
public interface RmiServerRemote extends Remote {

    /**
     * Requests registration of the given username for this client.
     *
     * @param username the desired username; uniqueness is enforced server-side
     * @throws RemoteException if the RMI call fails
     */
    void requestSetUsername(String username) throws RemoteException;

    /**
     * Requests creation of a new lobby with the specified number of players.
     *
     * @param numPlayers the expected number of players (2–5)
     * @throws RemoteException if the RMI call fails
     */
    void requestCreateLobby(int numPlayers) throws RemoteException;

    /**
     * Requests to join an existing lobby.
     *
     * @param lobbyID the identifier of the lobby to join
     * @throws RemoteException if the RMI call fails
     */
    void requestJoinLobby(String lobbyID) throws RemoteException;

    /**
     * Requests selection of a totem colour for this player.
     *
     * @param color the desired {@link Totem} colour
     * @throws RemoteException if the RMI call fails
     */
    void requestSelectTotem(Totem color) throws RemoteException;

    /**
     * Requests to leave the current lobby.
     *
     * @throws RemoteException if the RMI call fails
     */
    void requestLeaveLobby() throws RemoteException;

    /**
     * Requests reconnection to an ongoing game after a disconnection.
     *
     * @param nickname the player's nickname, which must match the one registered
     *                 before disconnection
     * @param gameId   the identifier of the game to reconnect to
     * @throws RemoteException if the RMI call fails
     */
    void requestReconnect(String nickname, String gameId) throws RemoteException;

    /**
     * Moves the player's totem to the specified offer tile.
     *
     * @param tileID the letter identifying the target offer tile
     * @throws RemoteException if the RMI call fails
     */
    void moveTotem(char tileID) throws RemoteException;

    /**
     * Submits the player's card selection for action resolution.
     *
     * @param selectedIDs the ordered list of card IDs selected by the player
     * @throws RemoteException if the RMI call fails
     */
    void resolveActions(List<String> selectedIDs) throws RemoteException;

    /**
     * Heartbeat call sent periodically by the client to signal it is still reachable.
     *
     * @throws RemoteException if the RMI call fails
     */
    void ping() throws RemoteException;
}