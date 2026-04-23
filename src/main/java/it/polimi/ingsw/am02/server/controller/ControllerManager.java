package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.*;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;
import it.polimi.ingsw.am02.common.messages.events.error.*;
import it.polimi.ingsw.am02.server.controller.persistence.CommandLogger;
import it.polimi.ingsw.am02.server.controller.persistence.GameLogger;
import it.polimi.ingsw.am02.server.controller.persistence.NoOpCommandLogger;
import it.polimi.ingsw.am02.server.model.Game;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerManager {

    private static final ControllerManager INSTANCE = new ControllerManager();

    // Internal maps  (all keyed by clientId, except gameNicknameToClient)
    private final Map<String, VirtualView> connectedClients;
    private final Map<String, Lobby> lobbies;
    private final Map<String, GameController> controllers;
    private final Map<String, String> clientToLobby;
    private final Map<String, String> clientToGame;
    private final Map<String, String> clientToNickname;
    private final Map<String, Map<String, String>> gameNicknameToClient;

    private ControllerManager() {
        this.connectedClients = new ConcurrentHashMap<>();
        this.lobbies = new ConcurrentHashMap<>();
        this.controllers = new ConcurrentHashMap<>();
        this.clientToLobby = new ConcurrentHashMap<>();
        this.clientToGame = new ConcurrentHashMap<>();
        this.clientToNickname = new ConcurrentHashMap<>();
        this.gameNicknameToClient = new ConcurrentHashMap<>();
    }

    public static ControllerManager getInstance() {
        return INSTANCE;
    }

    // Connection / disconnection entry points (called by network layer)

    public synchronized String handleClientConnected(VirtualView view) {
        String clientId = UUID.randomUUID().toString();
        connectedClients.put(clientId, view);

        view.notify(new UpdatedLobbiesEvent(getLobbyInfoList()));
        System.out.println("[ControllerManager] Client connected: " + clientId);
        return clientId;
    }

    public synchronized void handleDisconnection(String clientId) {
        if (clientToGame.containsKey(clientId)) {
            String gameId = clientToGame.get(clientId);
            String nickname = clientToNickname.get(clientId);
            GameController controller = controllers.get(gameId);
            if (controller != null && nickname != null) {
                controller.handlePlayerDisconnected(nickname);
            }
        } else if (clientToLobby.containsKey(clientId)) {
            leaveLobbyInternal(clientId);
        } else {
            connectedClients.remove(clientId);
        }
    }

    public synchronized void handleReconnect(String newClientId, VirtualView newView, String gameId, String nickname) {
        GameController controller = controllers.get(gameId);
        if (controller == null) {
            newView.notify(new ErrorEvent("Game not found or already ended: " + gameId));
            return;
        }

        Map<String, String> nicknameMap = gameNicknameToClient.get(gameId);
        if (nicknameMap == null || !nicknameMap.containsKey(nickname)) {
            newView.notify(new ErrorEvent("Nickname not found in game: " + nickname));
            return;
        }

        // Finds and invalidate the old clientId
        String oldClientId = nicknameMap.get(nickname);
        clientToGame.remove(oldClientId);
        clientToNickname.remove(oldClientId);
        connectedClients.remove(oldClientId);

        // Registers the new clientId in its place
        nicknameMap.put(nickname, newClientId);
        clientToGame.put(newClientId, gameId);
        clientToNickname.put(newClientId, nickname);

        System.out.println("[ControllerManager] Client reconnected: " + nickname
                + " in game " + gameId + " (new clientId: " + newClientId + ")");

        controller.handlePlayerReconnected(nickname, newView);
    }

    // Lobby management (called by network layer or by Lobby callbacks)
    public synchronized void createLobby(String clientId, int numPlayers) {
        VirtualView view = connectedClients.get(clientId);
        if (view == null) {
            System.err.println("[ControllerManager] createLobby: unknown clientId " + clientId);
            return;
        }

        String lobbyId = UUID.randomUUID().toString();
        Lobby lobby = new Lobby(lobbyId, numPlayers, this);
        lobbies.put(lobbyId, lobby);

        connectedClients.remove(clientId);
        clientToLobby.put(clientId, lobbyId);

        lobby.addClient(clientId, view);
        broadcastToPreLobbyClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }

    /**
     * Joins an existing lobby. The client is moved from {@link #connectedClients}
     * into the lobby; they will pick their nickname next.
     *
     * @param clientId the joining client's clientId
     * @param lobbyId  the target lobby's identifier
     */
    public synchronized void joinLobby(String clientId, String lobbyId) {
        VirtualView view = connectedClients.get(clientId);
        if (view == null) {
            System.err.println("[ControllerManager] joinLobby: unknown clientId " + clientId);
            return;
        }

        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) {
            view.notify(new ErrorEvent("Lobby not found: " + lobbyId));
            return;
        }
        if (lobby.isFull()) {
            view.notify(new ErrorEvent("Lobby is full"));
            return;
        }
        if (lobby.hasStarted()) {
            view.notify(new ErrorEvent("Lobby has already started"));
            return;
        }

        connectedClients.remove(clientId);
        clientToLobby.put(clientId, lobbyId);
        lobby.addClient(clientId, view);
        broadcastToPreLobbyClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }

    public synchronized void requestSetUsernameInLobby(String clientId, String nickname) {
        String lobbyId = clientToLobby.get(clientId);
        if (lobbyId == null) {
            VirtualView view = connectedClients.get(clientId);
            if (view != null) view.notify(new ErrorEvent("You are not in any lobby"));
            return;
        }

        if (nickname == null || nickname.isBlank()) {
            VirtualView view = getViewForClient(clientId);
            if (view != null)
                view.notify(new UsernameResultEvent(nickname, false, "Nickname cannot be empty"));
            return;
        }

        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) return;

        // Uniqueness check is delegated to Lobby (it knows its own players).
        boolean accepted = lobby.requestNickname(clientId, nickname);
        if (accepted) {
            clientToNickname.put(clientId, nickname);
        }
    }

    public synchronized void selectTotem(String clientId, Totem totem) {
        String lobbyId = clientToLobby.get(clientId);
        if (lobbyId == null) {
            VirtualView view = connectedClients.get(clientId);
            if (view != null) view.notify(new ErrorEvent("You are not in any lobby"));
            return;
        }
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) return;
        lobby.selectTotem(clientId, totem);
    }

    public synchronized void leaveLobby(String clientId) {
        leaveLobbyInternal(clientId);
    }

    // Callbacks from Lobby
    public synchronized void startGame(String gameId,
                                       List<String> clientIds,
                                       List<String> nicknames,
                                       Map<String, VirtualView> views,
                                       Map<String, Totem> chosenTotems) {

        views.values().forEach(v -> v.notify(new GameStartedEvent(gameId)));

        lobbies.remove(gameId);
        clientIds.forEach(clientToLobby::remove);

        // Build the nickname → clientId reverse map for this game.
        Map<String, String> nicknameToClient = new HashMap<>();
        for (int i = 0; i < clientIds.size(); i++) {
            nicknameToClient.put(nicknames.get(i), clientIds.get(i));
        }
        gameNicknameToClient.put(gameId, nicknameToClient);

        // Build the nickname → VirtualView map that GameController expects.
        Map<String, VirtualView> nicknameToView = new HashMap<>();
        for (int i = 0; i < clientIds.size(); i++) {
            nicknameToView.put(nicknames.get(i), views.get(clientIds.get(i)));
        }

        clientIds.forEach(id -> clientToGame.put(id, gameId));

        long seed = new Random().nextLong();
        Game model = new Game(gameId, nicknames, chosenTotems, seed);

        GameLogger gameLogger;
        try {
            CommandLogger fileLogger = new CommandLogger(gameId);
            fileLogger.logGameInit(gameId, seed, nicknames);
            gameLogger = fileLogger;
        } catch (IOException e) {
            System.err.println("[ControllerManager] Failed to create CommandLogger, "
                    + "persistence disabled: " + e.getMessage());
            gameLogger = new NoOpCommandLogger();
        }

        GameController controller = new GameController(gameId, model, nicknameToView, gameLogger);
        controller.setGameEndedCallback(() -> removeGameController(gameId));
        controllers.put(gameId, controller);

        broadcastToPreLobbyClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
        model.startFSM();
    }

    public synchronized void removeLobby(String lobbyId) {
        lobbies.remove(lobbyId);
        broadcastToPreLobbyClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }

    public synchronized void returnClientToPreLobby(String clientId, VirtualView view) {
        clientToLobby.remove(clientId);
        clientToNickname.remove(clientId);
        connectedClients.put(clientId, view);
        view.notify(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }

    // Command routing (called by network layer)
    public void routeGameCommand(String clientId, Command cmd) {
        String gameId = clientToGame.get(clientId);
        if (gameId == null) return;

        GameController controller = controllers.get(gameId);
        if (controller == null) return;

        if (!(cmd instanceof GameCommand gameCmd)) return;

        String nickname = clientToNickname.get(clientId);
        if (nickname == null) return;

        controller.handle(gameCmd, nickname);
    }


    // Lifecycle
    public synchronized void shutdown() {
        controllers.values().forEach(GameController::shutdown);
        controllers.clear();
    }

    // Helper methods
    private void leaveLobbyInternal(String clientId) {
        String lobbyId = clientToLobby.get(clientId);
        if (lobbyId == null) return;
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) return;
        lobby.removeClient(clientId);
    }

    private synchronized void removeGameController(String gameId) {
        if (controllers.remove(gameId) == null) return;
        gameNicknameToClient.remove(gameId);
        clientToGame.entrySet().removeIf(e -> gameId.equals(e.getValue()));
        System.out.println("[ControllerManager] Game removed: " + gameId);
        broadcastToPreLobbyClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }

    private void broadcastToPreLobbyClients(Event event) {
        connectedClients.values().forEach(v -> v.notify(event));
    }

    private List<LobbyInfo> getLobbyInfoList() {
        return lobbies.values().stream()
                .map(Lobby::toLobbyInfo)
                .toList();
    }

    private VirtualView getViewForClient(String clientId) {
        VirtualView v = connectedClients.get(clientId);
        if (v != null) return v;
        // Client might be in a lobby — ask the lobby.
        String lobbyId = clientToLobby.get(clientId);
        if (lobbyId == null) return null;
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) return null;
        return lobby.getView(clientId);
    }
}