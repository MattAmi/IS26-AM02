package it.polimi.ingsw.am02.client.controller;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;
import java.util.Map;

/**
 * Abstract controller that handles core logic and model synchronization.
 *
 * <p>Performs client-side validation only for structurally invalid input and
 * state that is stable enough to be read reliably from the local model
 * (e.g. totem availability, card presence on board). Rule enforcement — turn
 * order, phase constraints, pick limits — is delegated entirely to the server,
 * which is always the authoritative arbiter.
 */
public abstract class ClientController {

    protected ServerProxy proxy;
    protected final LobbyModel lobbyModel;
    protected final ClientView view;
    protected GameModel gameModel;

    public ClientController(ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        this.proxy = proxy;
        this.lobbyModel = lobbyModel;
        this.view = view;
        this.lobbyModel.addObserver(view);
    }

    public void setServerProxy(ServerProxy proxy) { this.proxy = proxy; }
    public void setGameModel(GameModel gameModel) { this.gameModel = gameModel; }

    // -------------------------------------------------------------------------
    // CONTEXT MANAGEMENT
    // -------------------------------------------------------------------------

    public synchronized void onGameModelRequired(String nickname) {
        if (this.gameModel != null)
            return;

        this.gameModel = new GameModel(nickname);
        this.lobbyModel.removeObserver(this.view);
        this.gameModel.addObserver(this.view);
        this.view.setGameModel(this.gameModel);
    }

    /**
     * Tears down the game context and attempts to reconnect to the lobby.
     * Handles network failures gracefully by notifying the view.
     */
    protected void performReturnToLobby() {
        try {
            if (gameModel != null) {
                gameModel.removeObserver(view);
                gameModel = null;
            }
            proxy.disconnect();
            proxy.connect();
            lobbyModel.addObserver(view);
            view.onReturnToLobby();
        } catch (Exception e) {
            view.onError("Return to lobby failed: The server is unreachable. " +
                    "Please check your connection and try again.");
            System.err.println("[ClientController] Error during return to lobby: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LOBBY PHASE HANDLERS
    // -------------------------------------------------------------------------

    /**
     * Validates that the nickname is non-blank and not already taken in the
     * current lobby, then forwards the request to the server.
     * Uniqueness across all connected clients is enforced server-side.
     */
    public void handleSetNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            view.onError("Action blocked: Nickname cannot be empty.");
            return;
        }
        if (gameModel != null) {
            view.onError("Action blocked: You cannot change your nickname while a game is in progress.");
            return;
        }

        LobbyInfo current = lobbyModel.getCurrentLobby();
        if (current != null && current.currentPlayers().contains(nickname)) {
            view.onError("Action blocked: The nickname '" + nickname + "' is already taken in this lobby.");
            return;
        }

        proxy.requestSetUsername(nickname);
    }

    /**
     * Validates lobby size constraints locally, then forwards to the server.
     */
    public void handleCreateLobby(int size) {
        if (size < 2 || size > 5) {
            view.onError("Action blocked: Lobby size must be between 2 and 5 players.");
            return;
        }
        if (lobbyModel.getCurrentLobby() != null || gameModel != null) {
            view.onError("Action blocked: You must leave your current lobby or game before creating a new one.");
            return;
        }
        proxy.requestCreateLobby(size);
    }

    /**
     * Validates the lobby index against the locally known list, then forwards to the server.
     */
    public void handleJoinLobby(int index) {
        if (lobbyModel.getCurrentLobby() != null || gameModel != null) {
            view.onError("Action blocked: You are already in a lobby or game.");
            return;
        }
        List<LobbyInfo> available = lobbyModel.getAvailableLobbies();
        if (available.isEmpty()) {
            view.onError("Action blocked: No lobbies are currently available.");
            return;
        }
        if (index < 0 || index >= available.size()) {
            view.onError("Action blocked: Invalid index. Please choose a value between 0 and "
                    + (available.size() - 1) + ".");
            return;
        }
        proxy.requestJoinLobby(available.get(index).lobbyId());
    }

    /**
     * Validates that the totem value is non-null and not already taken according
     * to the locally known lobby state, then forwards to the server.
     *
     * <p>Totem availability is stable: it changes only when another player
     * explicitly selects one, and the corresponding event is delivered immediately.
     */
    public void handleSelectTotem(Totem totem) {
        if (totem == null) {
            view.onError("Action blocked: Invalid totem color.");
            return;
        }
        if (lobbyModel.getMyNickname() == null) {
            view.onError("Action blocked: You must set a nickname before choosing a totem.");
            return;
        }
        LobbyInfo current = lobbyModel.getCurrentLobby();
        if (current == null) {
            view.onError("Action blocked: You must be in a lobby to select a totem.");
            return;
        }

        String taker = current.chosenTotems().entrySet().stream()
                .filter(e -> e.getValue().equals(totem))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (taker != null) {
            view.onError("Action blocked: Totem " + totem + " is already taken by " + taker + ".");
            return;
        }

        proxy.requestSelectTotem(totem);
    }

    // -------------------------------------------------------------------------
    // IN-GAME HANDLERS
    // -------------------------------------------------------------------------

    /**
     * Validates that a game is in progress and the target tile exists structurally,
     * then forwards the move-totem request. Turn order and phase constraints are
     * enforced server-side.
     *
     * <p>Tile existence is stable: the offer tiles are initialized at game setup
     * and never change. The special tile 'T' (totem return) is always valid.
     */
    public void handleMoveTotem(char tileID) {
        if (gameModel == null) {
            view.onError("Action blocked: No game in progress.");
            return;
        }

        if (tileID != 'T') {
            boolean tileExists = gameModel.getOfferTiles().stream()
                    .anyMatch(t -> t.tileID() == tileID);
            if (!tileExists) {
                view.onError("Action blocked: Tile '" + tileID + "' does not exist.");
                return;
            }
        }

        proxy.moveTotem(tileID);
    }

    /**
     * Validates that each selected card is currently present on the board
     * according to the local model, then forwards the request.
     *
     * <p>Board contents are stable within a turn: they change only at end-of-round
     * via {@code notifyBoardUpdated}, making this check reliable.
     * Pick limits and phase constraints are enforced server-side.
     */
    public void handleResolveActions(List<String> cardIDs) {
        if (gameModel == null) {
            view.onError("Action blocked: No game in progress.");
            return;
        }
        for (String id : cardIDs) {
            boolean exists = gameModel.getUpperRow().contains(id)
                    || gameModel.getLowerRow().contains(id)
                    || gameModel.getUpperRowBuildings().contains(id)
                    || gameModel.getLowerRowBuildings().contains(id);
            if (!exists) {
                view.onError("Action blocked: Card [" + id + "] is not on board.");
                return;
            }
        }
        proxy.resolveActions(cardIDs);
    }

    /**
     * Validates that nickname and game ID are non-blank, then forwards the
     * reconnection request.
     */
    public void handleReconnect(String nickname, String gameId) {
        if (nickname == null || nickname.isBlank()) {
            view.onError("Action blocked: Nickname is required for reconnection.");
            return;
        }
        if (gameId == null || gameId.isBlank()) {
            view.onError("Action blocked: Game ID is required for reconnection.");
            return;
        }
        proxy.requestReconnect(nickname, gameId);
    }

    public synchronized GameModel getGameModel() { return gameModel; }
}