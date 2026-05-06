package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the client-side state of the pre-game lobby phase.
 * Receives lobby-related events from the ServerProxy and pushes
 * granular updates to registered {@link ClientView} observers.
 */
public class LobbyModel {

    private String myNickname;
    private List<LobbyInfo> availableLobbies = new ArrayList<>();
    private LobbyInfo currentLobby;
    private final List<ClientView> clientViews = new ArrayList<>();

    // OBSERVERS
    public synchronized void addObserver(ClientView observer) { clientViews.add(observer); }
    public synchronized void removeObserver(ClientView observer) { clientViews.remove(observer); }

    // DOMAIN UPDATES

    /**
     * Applies the result of a username request: stores the accepted nickname
     * and notifies views.
     *
     * @param username the requested username
     * @param isValid  whether the server accepted it
     * @param reason   rejection reason, or {@code null} if accepted
     */
    public synchronized void updateUsernameResult(String username, boolean isValid, String reason) {
        if (isValid) this.myNickname = username;
        clientViews.forEach(o -> o.onUsernameResult(username, isValid, reason));
    }

    /**
     * Applies a full lobby list update. Resets local lobby and nickname state
     * to reflect that the client is back in the pre-lobby phase.
     *
     * @param lobbies the current list of available lobbies
     */
    public synchronized void updateAvailableLobbies(List<LobbyInfo> lobbies) {
        this.currentLobby = null;
        this.myNickname = null;
        this.availableLobbies = new ArrayList<>(lobbies);
        clientViews.forEach(o -> o.onAvailableLobbiesUpdated(lobbies));
    }

    /**
     * Applies a lobby state update for the lobby the client is currently in.
     *
     * @param lobby the updated lobby info
     */
    public synchronized void updateCurrentLobby(LobbyInfo lobby) {
        this.currentLobby = lobby;
        clientViews.forEach(o -> o.onCurrentLobbyUpdated(lobby));
    }

    /**
     * Applies a lobby dissolution event: clears the current lobby reference
     * and notifies views.
     */
    public synchronized void updateLobbyDissolved() {
        this.currentLobby = null;
        clientViews.forEach(ClientView::onLobbyDissolved);
    }

    /**
     * Applies an error event: forwards the message to all registered views.
     *
     * @param errorMessage the error message from the server
     */
    public synchronized void updateError(String errorMessage) {
        clientViews.forEach(o -> o.onError(errorMessage));
    }

    // GETTERS

    public synchronized String getMyNickname() { return myNickname; }
    public synchronized List<LobbyInfo> getAvailableLobbies() { return availableLobbies; }
    public synchronized LobbyInfo getCurrentLobby() { return currentLobby; }
}