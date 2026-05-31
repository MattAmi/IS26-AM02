package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the client-side state of the pre-game lobby phase.
 *
 * <p>Receives lobby-related notifications from
 * {@link it.polimi.ingsw.am02.client.network.ClientNetworkDispatcher}
 * and pushes granular updates to all registered
 * {@link it.polimi.ingsw.am02.client.view.ClientView} observers.
 *
 * <p>All public methods are {@code synchronized} on the model's own monitor.
 */
public class LobbyModel {

    private String myNickname;
    private List<LobbyInfo> availableLobbies = new ArrayList<>();
    private LobbyInfo currentLobby;
    private final List<ClientView> clientViews = new ArrayList<>();

    // OBSERVERS

    /**
     * Registers a {@link it.polimi.ingsw.am02.client.view.ClientView} to receive
     * future lobby updates. No-op if the observer is already registered.
     *
     * @param observer the view to add
     */
    public synchronized void addObserver(ClientView observer) {
        if (!clientViews.contains(observer)) {
            clientViews.add(observer);
        }
    }

    /**
     * Removes a previously registered observer.
     *
     * @param observer the view to remove
     */
    public synchronized void removeObserver(ClientView observer) { clientViews.remove(observer); }

    // DOMAIN UPDATES

    /**
     * Applies the result of a username request: stores the accepted nickname
     * and notifies all views.
     *
     * @param username the requested username
     * @param isValid  {@code true} if the server accepted the nickname
     * @param reason   the rejection reason, or {@code null} if accepted
     */
    public synchronized void updateUsernameResult(String username, boolean isValid, String reason) {
        if (isValid) this.myNickname = username;
        clientViews.forEach(o -> o.onUsernameResult(username, isValid, reason));
    }

    /**
     * Applies a full lobby list update received from the server.
     * Also resets the local current-lobby and nickname references to reflect
     * that this client is now in the pre-lobby state.
     *
     * @param lobbies the current list of available open lobbies
     */
    public synchronized void updateAvailableLobbies(List<LobbyInfo> lobbies) {
        this.currentLobby = null;
        this.myNickname = null;
        this.availableLobbies = new ArrayList<>(lobbies);
        clientViews.forEach(o -> o.onAvailableLobbiesUpdated(lobbies));
    }

    /**
     * Applies an incremental update to the lobby this client is currently in.
     *
     * @param lobby the updated lobby state
     */
    public synchronized void updateCurrentLobby(LobbyInfo lobby) {
        this.currentLobby = lobby;
        clientViews.forEach(o -> o.onCurrentLobbyUpdated(lobby));
    }

    /**
     * Applies a lobby dissolution event: clears the current lobby reference
     * and notifies all views.
     */
    public synchronized void updateLobbyDissolved() {
        this.currentLobby = null;
        clientViews.forEach(ClientView::onLobbyDissolved);
    }

    /**
     * Applies an error notification from the server: forwards the message
     * to all registered views.
     *
     * @param errorMessage the human-readable error description
     */
    public synchronized void updateError(String errorMessage) {
        clientViews.forEach(o -> o.onError(errorMessage));
    }

    // GETTERS

    /**
     * @return the nickname accepted by the server for this client,
     *         or {@code null} if no nickname has been set yet
     */
    public synchronized String getMyNickname() { return myNickname; }

    /**
     * @return the most recent list of available lobbies received from the server;
     *         empty if the client has not yet received any update
     */
    public synchronized List<LobbyInfo> getAvailableLobbies() { return availableLobbies; }


    /**
     * @return the {@link it.polimi.ingsw.am02.common.dto.LobbyInfo} for the lobby
     *         this client is currently in, or {@code null} if not in any lobby
     */
    public synchronized LobbyInfo getCurrentLobby() { return currentLobby; }
}