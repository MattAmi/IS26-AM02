package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;
import it.polimi.ingsw.am02.common.messages.events.error.*;

import java.util.*;

public class Lobby {

    private final String lobbyId;
    private final int expectedPlayers;
    private final String hostNickname;
    private final List<String> nicknames;
    private final Map<String, VirtualView> views;
    private final Map<String, Totem> chosenTotems;
    private final ControllerManager controllerManager;
    private boolean started; // Set to true once startGame has been triggered (it prevents double-start).

    public Lobby(String lobbyId, int expectedPlayers, String hostNickname, ControllerManager controllerManager) {
        this.lobbyId = lobbyId;
        this.expectedPlayers = expectedPlayers;
        this.hostNickname = hostNickname;
        this.controllerManager = controllerManager;
        this.started = false;

        this.nicknames = new ArrayList<>();
        this.views = new LinkedHashMap<>();
        this.chosenTotems = new LinkedHashMap<>();
    }

    // Package-private methods used by ControllerManager

    boolean isFull() {
        return nicknames.size() == expectedPlayers;
    }

    boolean hasStarted() {
        return started;
    }


    // Lobby's snapshot
    synchronized LobbyInfo toLobbyInfo() {
        return new LobbyInfo(lobbyId, expectedPlayers, List.copyOf(nicknames), Map.copyOf(chosenTotems));
    }


    // Lifecycle methods

    public synchronized void addPlayer(String nickname, VirtualView view) {
        if (isFull() || started)
            return;

        nicknames.add(nickname);
        views.put(nickname, view);
        broadcast(new UpdatedLobbyEvent(toLobbyInfo()));
    }

    public synchronized void selectTotem(String nickname, Totem totem) {
        if (!nicknames.contains(nickname)) return; // not in this lobby

        String currentOwner = null;
        for (Map.Entry<String, Totem> entry : chosenTotems.entrySet()) {
            if (entry.getValue() == totem && !entry.getKey().equals(nickname)) {
                currentOwner = entry.getKey();
                break;
            }
        }
        if (currentOwner != null) {
            views.get(nickname).notify(new ErrorEvent("Totem already taken by " + currentOwner));
            return;
        }

        chosenTotems.put(nickname, totem); // overwrites previous choice if any
        broadcast(new UpdatedLobbyEvent(toLobbyInfo()));

        if (isReadyToStart()) {
            started = true;
            controllerManager.startGame(lobbyId, List.copyOf(nicknames), new LinkedHashMap<>(views), new LinkedHashMap<>(chosenTotems));
        }
    }

    public synchronized void removePlayer(String nickname) {
        if (!nicknames.contains(nickname)) return; // not in this lobby

        VirtualView leavingView = views.get(nickname);

        if (nickname.equals(hostNickname)) {
            dissolve();
        } else {
            nicknames.remove(nickname);
            views.remove(nickname);
            chosenTotems.remove(nickname);

            controllerManager.returnPlayerToLobbySelection(nickname, leavingView);

            if (nicknames.isEmpty()) {
                controllerManager.removeLobby(lobbyId);
            } else {
                broadcast(new UpdatedLobbyEvent(toLobbyInfo()));
            }
        }
    }

    public synchronized void dissolve() {
        broadcast(new LobbyDissolvedEvent(lobbyId));

        // Returns all members to lobby selection before clearing state.
        new ArrayList<>(nicknames)
                .forEach(n -> controllerManager.returnPlayerToLobbySelection(n, views.get(n)));

        nicknames.clear();
        views.clear();
        chosenTotems.clear();
        controllerManager.removeLobby(lobbyId);
    }


    // Private helpers
    private boolean isReadyToStart() {
        return nicknames.size() == expectedPlayers && chosenTotems.size() == expectedPlayers;
    }

    private void broadcast(Event event) {
        views.values().forEach(v -> v.notify(event));
    }
}