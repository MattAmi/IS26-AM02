package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.error.ErrorEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;

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

    public void addObserver(ClientView observer) { clientViews.add(observer); }
    public void removeObserver(ClientView observer) { clientViews.remove(observer); }

    //  EVENT DISPATCH
    public void apply(Event event) {
        switch (event) {
            case UsernameResultEvent e -> {
                if (e.isValid())
                    this.myNickname = e.username();
                clientViews.forEach(o -> o.onUsernameResult(e.username(), e.isValid(), e.reason()));
            }

            case UpdatedLobbiesEvent e -> {
                this.availableLobbies = new ArrayList<>(e.lobbies());
                clientViews.forEach(o -> o.onAvailableLobbiesUpdated(e.lobbies()));
            }

            case UpdatedLobbyEvent e -> {
                this.currentLobby = e.lobby();
                clientViews.forEach(o -> o.onCurrentLobbyUpdated(e.lobby()));
            }

            case LobbyDissolvedEvent ignored -> {
                this.currentLobby = null;
                clientViews.forEach(ClientView::onLobbyDissolved);
            }

            case ErrorEvent e -> {
                clientViews.forEach(o -> o.onError(e.errorMessage()));
            }

            default -> {}
        }
    }

    public String getMyNickname() { return myNickname; }
    public List<LobbyInfo> getAvailableLobbies() { return availableLobbies; }
    public LobbyInfo getCurrentLobby() { return currentLobby; }
}