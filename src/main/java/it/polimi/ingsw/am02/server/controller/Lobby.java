package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

import java.util.*;

/**
 * Represents a pre-game lobby in which players gather before the match starts.
 *
 * <p>A lobby is created by {@link ControllerManager} and holds up to
 * {@code expectedPlayers} clients. It tracks nickname selection and totem
 * selection independently. The game starts automatically once every slot is
 * filled with a valid nickname and a chosen totem, via
 * {@link ControllerManager#startGame}.
 *
 * <p>All public methods are {@code synchronized} on the lobby's own monitor.
 */
public class Lobby {

    private final String lobbyId;
    private final int expectedPlayers;
    private final ControllerManager controllerManager;

    private final List<String> clientIds;
    private final Map<String, VirtualView> views;
    private final Map<String, String> clientToNickname;
    private final Map<String, Totem> chosenTotems;

    private boolean started;

    /**
     * @param lobbyId           unique identifier for this lobby (reused as the game ID)
     * @param expectedPlayers   total number of players required to start (2–5)
     * @param controllerManager reference to the singleton controller manager,
     *                          used to trigger game start and return clients on leave
     */
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

    /**
     * Builds and returns an immutable snapshot of this lobby's current state.
     *
     * @return a {@link it.polimi.ingsw.am02.common.dto.LobbyInfo} reflecting the
     *         current nicknames and chosen totems
     */
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
    /**
     * Adds a client to the lobby and broadcasts the updated lobby state.
     * No-op if the lobby is already full or has started.
     *
     * @param clientId the client's unique identifier
     * @param view     the client's {@link it.polimi.ingsw.am02.common.interfaces.VirtualView}
     */
    public synchronized void addClient(String clientId, VirtualView view) {
        if (isFull() || started) return;

        clientIds.add(clientId);
        views.put(clientId, view);
        broadcastCurrentLobbyUpdated();
    }

    /**
     * Processes a nickname request from a client.
     * Rejects the request if the nickname is blank or already taken in this lobby.
     * Triggers {@link #checkAndStart()} after a successful assignment.
     *
     * @param clientId the requesting client
     * @param nickname the desired nickname
     * @return {@code true} if the nickname was accepted, {@code false} otherwise
     */
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

    /**
     * Processes a totem selection from a client.
     * Rejects the request if the client has no nickname yet or the totem is already taken.
     * Triggers {@link #checkAndStart()} after a successful selection.
     *
     * @param clientId the requesting client
     * @param totem    the desired totem colour
     */
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

    /**
     * Removes a client from the lobby and returns them to the pre-lobby state.
     * Dissolves the lobby if it becomes empty.
     *
     * @param clientId the client leaving the lobby
     */
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
            controllerManager.notifyLobbyListChanged();
        }
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

    /** @return the current number of clients in this lobby */
    public int getPlayerCount() { return views.size(); }
}