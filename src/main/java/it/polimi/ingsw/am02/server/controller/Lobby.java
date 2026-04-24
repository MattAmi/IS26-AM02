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
    private final ControllerManager controllerManager;

    private final List<String> clientIds;
    private final Map<String, VirtualView> views;
    private final Map<String, String> clientToNickname;
    private final Map<String, Totem> chosenTotems;

    private boolean started;


    public Lobby(String lobbyId, int expectedPlayers, ControllerManager controllerManager) {
        this.lobbyId = lobbyId;
        this.expectedPlayers = expectedPlayers;
        this.controllerManager = controllerManager;
        this.started = false;

        this.clientIds = new ArrayList<>();
        this.views = new LinkedHashMap<>();
        this.clientToNickname = new LinkedHashMap<>();
        this.chosenTotems = new LinkedHashMap<>();
    }

    // Package-private accessors (used by ControllerManager)

    boolean isFull() { return clientIds.size() == expectedPlayers; }

    boolean hasStarted() { return started; }

    VirtualView getView(String clientId) { return views.get(clientId); }

    synchronized LobbyInfo toLobbyInfo() {

        List<String> nicknameList = clientIds.stream()
                .map(id -> clientToNickname.getOrDefault(id, ""))
                .toList();

        Map<String, Totem> nicknameToTotem = new LinkedHashMap<>();

        for (String id : clientIds) {
            String nick = clientToNickname.get(id);
            Totem totem = chosenTotems.get(id);
            if (nick != null && totem != null) {
                nicknameToTotem.put(nick, totem);
            }
        }

        return new LobbyInfo(lobbyId, expectedPlayers, nicknameList, nicknameToTotem);
    }

    // Lifecycle
    public synchronized void addClient(String clientId, VirtualView view) {
        if (isFull() || started) return;

        clientIds.add(clientId);
        views.put(clientId, view);
        broadcast(new UpdatedLobbyEvent(toLobbyInfo()));
    }

    public synchronized boolean requestNickname(String clientId, String nickname) {
        VirtualView view = views.get(clientId);
        if (view == null) return false;

        if (nickname == null || nickname.isBlank()) {
            view.notify(new UsernameResultEvent(nickname, false, "Nickname cannot be empty"));
            return false;
        }

        boolean alreadyTaken = clientToNickname.values().stream()
                .anyMatch(existing -> existing.equals(nickname));
        if (alreadyTaken) {
            view.notify(new UsernameResultEvent(nickname, false,
                    "Nickname already taken in this lobby"));
            return false;
        }

        clientToNickname.put(clientId, nickname);
        view.notify(new UsernameResultEvent(nickname, true, null));
        broadcast(new UpdatedLobbyEvent(toLobbyInfo()));

        checkAndStart();
        return true;
    }

    public synchronized void selectTotem(String clientId, Totem totem) {
        if (!clientIds.contains(clientId)) return;

        if (totem == null) {
            views.get(clientId).notify(new ErrorEvent("Invalid totem selection"));
            return;
        }

        // Check if this totem is already taken by someone else.
        for (Map.Entry<String, Totem> entry : chosenTotems.entrySet()) {
            if (entry.getValue() == totem && !entry.getKey().equals(clientId)) {
                String takenByNickname = clientToNickname.getOrDefault(
                        entry.getKey(), entry.getKey());

                views.get(clientId).notify(new ErrorEvent("Totem already taken by " + takenByNickname));

                return;
            }
        }

        chosenTotems.put(clientId, totem);
        broadcast(new UpdatedLobbyEvent(toLobbyInfo()));

        checkAndStart();
    }

    public synchronized void removeClient(String clientId) {
        if (!clientIds.contains(clientId)) return;

        VirtualView leavingView = views.get(clientId);

        clientIds.remove(clientId);
        views.remove(clientId);
        clientToNickname.remove(clientId);
        chosenTotems.remove(clientId);

        controllerManager.returnClientToPreLobby(clientId, leavingView);

        if (clientIds.isEmpty()) {
            controllerManager.removeLobby(lobbyId);
        } else {
            broadcast(new UpdatedLobbyEvent(toLobbyInfo()));
        }
    }

    public synchronized void dissolve() {
        broadcast(new LobbyDissolvedEvent(lobbyId));

        // Iterate over a copy to avoid ConcurrentModificationException.
        new ArrayList<>(clientIds)
                .forEach(id -> controllerManager.returnClientToPreLobby(id, views.get(id)));

        clientIds.clear();
        views.clear();
        clientToNickname.clear();
        chosenTotems.clear();

        controllerManager.removeLobby(lobbyId);
    }


    // Helper methods

    private void checkAndStart() {
        if (started || !isReadyToStart()) return;

        started = true;

        // Build the ordered parallel lists that ControllerManager expects.
        List<String> orderedClientIds = List.copyOf(clientIds);
        List<String> orderedNicknames = clientIds.stream()
                .map(clientToNickname::get)
                .toList();

        // nickname → Totem (keyed by nickname, as GameController expects)
        Map<String, Totem> nicknameToTotem = new LinkedHashMap<>();
        for (String id : clientIds) {
            nicknameToTotem.put(clientToNickname.get(id), chosenTotems.get(id));
        }

        // clientId → VirtualView
        Map<String, VirtualView> viewsCopy = new LinkedHashMap<>(views);

        controllerManager.startGame(
                lobbyId,
                orderedClientIds,
                orderedNicknames,
                viewsCopy,
                nicknameToTotem
        );
    }

    private boolean isReadyToStart() {
        return clientIds.size() == expectedPlayers
                && clientToNickname.size() == expectedPlayers
                && chosenTotems.size() == expectedPlayers;
    }

    private void broadcast(Event event) {
        views.values().forEach(v -> v.notify(event));
    }
}