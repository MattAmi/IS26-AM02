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
import it.polimi.ingsw.am02.server.model.Game;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerManager {

    private static final ControllerManager INSTANCE = new ControllerManager();
    private final Map<String, VirtualView> connectedClients; // Authenticated clients not yet in a game (in lobby selection or in a lobby).
    private final Map<String, Lobby> lobbies; // Active lobbies awaiting enough players to start.
    private final Map<String, GameController> controllers; // Active game controllers, keyed by gameId.
    private final Map<String, String> playerToLobby; // Maps each player nickname to the lobbyId they are currently in.
    private final Map<String, String> playerToGame; //Maps each player nickname to the gameId of their active game.

    private ControllerManager() {
        this.connectedClients = new ConcurrentHashMap<>();
        this.lobbies = new ConcurrentHashMap<>();
        this.controllers = new ConcurrentHashMap<>();
        this.playerToLobby = new ConcurrentHashMap<>();
        this.playerToGame = new ConcurrentHashMap<>();
    }

    public static ControllerManager getInstance() {
        return INSTANCE;
    }

    // Authentication

    public synchronized void requestSetUsername(String nickname, VirtualView view) {
        //A nickname is valid if it is not blank, and it is not already taken by another
        // authenticated client (in lobby selection, in a lobby, or in a game).

        if (nickname == null || nickname.isBlank()) {
            view.notify(new UsernameResultEvent(nickname, false, "Nickname cannot be empty"));
            return;
        }
        boolean alreadyTaken = connectedClients.containsKey(nickname)
                || playerToLobby.containsKey(nickname)
                || playerToGame.containsKey(nickname);
        if (alreadyTaken) {
            view.notify(new UsernameResultEvent(nickname, false, "Nickname already taken"));
            return;
        }

        connectedClients.put(nickname, view);
        view.notify(new UsernameResultEvent(nickname, true, null));

        List<LobbyInfo> lobbyList = getLobbyInfoList();
        broadcastToLobbySelectionClients(new UpdatedLobbiesEvent(lobbyList));
    }


    // Lobby management

    // Creates a new lobby for the given host and assigns it a server-generated ID.
    public synchronized void createLobby(int expectedPlayers, String hostNickname, VirtualView hostView) {
        String lobbyId = UUID.randomUUID().toString();
        Lobby lobby = new Lobby(lobbyId, expectedPlayers, hostNickname, this);
        lobbies.put(lobbyId, lobby);

        connectedClients.remove(hostNickname);
        playerToLobby.put(hostNickname, lobbyId);

        lobby.addPlayer(hostNickname, hostView);

        broadcastToLobbySelectionClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }


    // Adds a player to an existing lobby.
    public synchronized void joinLobby(String lobbyId, String nickname, VirtualView view) {
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

        connectedClients.remove(nickname);
        playerToLobby.put(nickname, lobbyId);
        lobby.addPlayer(nickname, view);

        broadcastToLobbySelectionClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }

    // Handles a totem selection request from a player already in a lobby.
    public synchronized void selectTotem(String nickname, Totem totem) {
        String lobbyId = playerToLobby.get(nickname);

        if (lobbyId == null) {
            VirtualView view = connectedClients.get(nickname);
            if (view != null) view.notify(new ErrorEvent("You are not in any lobby"));
            return;
        }

        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) return;
        lobby.selectTotem(nickname, totem);
    }

    // Handles a leave-lobby request from a player currently in a lobby.
    public synchronized void leaveLobby(String nickname) {
        String lobbyId = playerToLobby.get(nickname);

        if (lobbyId == null)
            return;

        Lobby lobby = lobbies.get(lobbyId);

        if (lobby == null)
            return;
        lobby.removePlayer(nickname);
    }

    // Callbacks from Lobby

    // Called by Lobby when all players have joined and chosen a totem.
    public void startGame(String gameId, List<String> nicknames, Map<String, VirtualView> views, Map<String, Totem> chosenTotems) {
        views.values().forEach(v -> v.notify(new GameStartedEvent(gameId)));

        lobbies.remove(gameId);
        nicknames.forEach(playerToLobby::remove);

        Game model = new Game(gameId, nicknames, chosenTotems);
        GameController gameController = new GameController(model, views);
        controllers.put(gameId, gameController);
        nicknames.forEach(n -> playerToGame.put(n, gameId));

        broadcastToLobbySelectionClients(new UpdatedLobbiesEvent(getLobbyInfoList()));

        model.startFSM();
    }

    // Called by Lobby when it's removed
    public void removeLobby(String lobbyId) {
        lobbies.remove(lobbyId);
        broadcastToLobbySelectionClients(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }

    // Called by Lobby when a player has left and should be returned to the lobby-selection pool.
    public void returnPlayerToLobbySelection(String nickname, VirtualView view) {
        playerToLobby.remove(nickname);
        connectedClients.put(nickname, view);
        view.notify(new UpdatedLobbiesEvent(getLobbyInfoList()));
    }


    // Disconnection handling

    // Handles a client disconnection at any stage: lobby selection, in a lobby, or in a game.
    public synchronized void handleDisconnection(String nickname) {

        if (playerToGame.containsKey(nickname)) {
            String gameId = playerToGame.get(nickname);
            GameController gameController = controllers.get(gameId);
            if (gameController != null)
                gameController.handlePlayerDisconnected(nickname);

        } else if (playerToLobby.containsKey(nickname)) {
            leaveLobby(nickname);
        } else {
            connectedClients.remove(nickname);
        }
    }


    // Command routing (called by network layer)

    // Routes an incoming Command from a player in an active game to the correct GameController.
    public void routeGameCommand(String nickname, Command cmd) {
        String gameId = playerToGame.get(nickname);
        if (gameId == null)
            return;

        GameController gameController = controllers.get(gameId);

        if (gameController == null)
            return;

        if (!(cmd instanceof GameCommand gameCmd)) {
            return;
        }

        gameController.handle(gameCmd, nickname);
    }


    // Helper methods

    private void broadcastToLobbySelectionClients(Event event) {
        connectedClients.values().forEach(v -> v.notify(event));
    }

    private List<LobbyInfo> getLobbyInfoList() {
        return lobbies.values().stream()
                .map(Lobby::toLobbyInfo)
                .toList();
    }
}