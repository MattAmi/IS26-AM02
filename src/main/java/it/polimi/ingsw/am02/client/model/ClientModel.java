package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;
import java.util.ArrayList;
import java.util.List;

public class ClientModel implements VirtualView {
    private String myNickname;
    private List<LobbyInfo> availableLobbies = new ArrayList<>();
    private LobbyInfo currentLobby;
    private final List<ClientModelObserver> observers = new ArrayList<>();

    public void addObserver(ClientModelObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        observers.forEach(ClientModelObserver::update);
    }

    @Override
    public void notify(Event event) {
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.myNickname = e.username();
        } else if (event instanceof UpdatedLobbiesEvent e) {
            this.availableLobbies = e.lobbies();
        } else if (event instanceof UpdatedLobbyEvent e) {
            this.currentLobby = e.lobby();
        }

        notifyObservers();
    }

    public List<LobbyInfo> getAvailableLobbies() { return availableLobbies; }
    public LobbyInfo getCurrentLobby() { return currentLobby; }
    public String getMyNickname() { return myNickname; }
}