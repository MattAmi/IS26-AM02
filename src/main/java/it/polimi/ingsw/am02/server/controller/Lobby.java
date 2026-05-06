package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

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
        broadcastCurrentLobbyUpdated();
    }

    public synchronized boolean requestNickname(String clientId, String nickname) {
        VirtualView view = views.get(clientId);
        if (view == null) return false;

        if (nickname == null || nickname.isBlank()) {
            view.notifyUsernameResult(nickname, false, "Nickname cannot be empty");
            return false;
        }

        boolean alreadyTaken = clientToNickname.values().stream()
                .anyMatch(existing -> existing.equals(nickname));
        if (alreadyTaken) {
            view.notifyUsernameResult(nickname, false, "Nickname already taken in this lobby");
            return false;
        }

        clientToNickname.put(clientId, nickname);
        view.notifyUsernameResult(nickname, true, null);
        broadcastCurrentLobbyUpdated();

        checkAndStart();
        return true;
    }

    public synchronized void selectTotem(String clientId, Totem totem) {
        if (!clientIds.contains(clientId)) return;
        VirtualView view = views.get(clientId);

        if (!clientToNickname.containsKey(clientId)) {
            view.notifyError("You must choose a nickname before selecting a totem.");
            return;
        }

        if (totem == null) {
            view.notifyError("Invalid totem selection");
            return;
        }

        for (Map.Entry<String, Totem> entry : chosenTotems.entrySet()) {
            if (entry.getValue() == totem && !entry.getKey().equals(clientId)) {
                String takenByNickname = clientToNickname.getOrDefault(entry.getKey(), entry.getKey());
                view.notifyError("Totem already taken by " + takenByNickname);
                return;
            }
        }

        chosenTotems.put(clientId, totem);
        broadcastCurrentLobbyUpdated();

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
            broadcastCurrentLobbyUpdated();
        }
    }

    public synchronized void dissolve() {
        broadcastLobbyDissolved();

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

        List<String> orderedClientIds = List.copyOf(clientIds);
        List<String> orderedNicknames = clientIds.stream()
                .map(clientToNickname::get)
                .toList();

        Map<String, Totem> nicknameToTotem = new LinkedHashMap<>();
        for (String id : clientIds) {
            nicknameToTotem.put(clientToNickname.get(id), chosenTotems.get(id));
        }

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

    // --- Metodi di Broadcast granulari ---

    private void broadcastCurrentLobbyUpdated() {
        LobbyInfo info = toLobbyInfo();
        views.values().forEach(v -> v.notifyCurrentLobbyUpdated(info));
    }

    private void broadcastLobbyDissolved() {
        views.values().forEach(v -> v.notifyLobbyDissolved(lobbyId));
    }

    public int getPlayerCount() { return views.size(); }
}