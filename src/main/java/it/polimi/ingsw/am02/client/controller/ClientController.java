package it.polimi.ingsw.am02.client.controller;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Abstract controller that handles core logic and model synchronization.
 * Performs strict client-side validation to minimize network traffic and
 * improve User Experience.
 */
public abstract class ClientController {

    protected final ServerProxy proxy;
    protected final LobbyModel lobbyModel;
    protected final ClientView view;
    protected GameModel gameModel;

    public ClientController(ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        this.proxy = proxy;
        this.lobbyModel = lobbyModel;
        this.view = view;
        this.lobbyModel.addObserver(view);
    }

    // -------------------------------------------------------------------------
    // CONTEXT MANAGEMENT
    // -------------------------------------------------------------------------

    public void onGameModelRequired(String nickname) {
        if (this.gameModel != null) return;
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
            // 1. Local Cleanup: Unsubscribe view from the game model if it exists
            if (gameModel != null) {
                gameModel.removeObserver(view);
                gameModel = null;
            }

            // 2. Network Reset: Attempt to refresh the connection
            // This clears any stale session data on the server/proxy side
            proxy.disconnect();
            proxy.connect();

            // 3. State Restoration: Re-subscribe to the lobby model
            lobbyModel.addObserver(view);

            // 4. View Update: Tell the UI to switch back to the lobby screen
            view.onReturnToLobby();

        } catch (Exception e) {
            // 5. Exception Handling: If the server is unreachable during reconnection
            view.onError("Return to lobby failed: The server is unreachable. " +
                    "Please check your connection and try again.");

            System.err.println("[ClientController] Error during return to lobby: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LOBBY PHASE HANDLERS
    // -------------------------------------------------------------------------

    /**
     * Handles nickname requests with local validation for emptiness,
     * game status, and uniqueness within the current lobby.
     */
    public void handleSetNickname(String nickname) {
        if (gameModel != null) {
            view.onError("Action blocked: You cannot change your nickname while a game is in progress.");
            return;
        }
        if (nickname == null || nickname.isBlank()) {
            view.onError("Action blocked: Nickname cannot be empty.");
            return;
        }

        // Uniqueness check if already in a lobby
        LobbyInfo current = lobbyModel.getCurrentLobby();
        if (current != null && current.currentPlayers().contains(nickname)) {
            view.onError("Action blocked: The nickname '" + nickname + "' is already taken in this lobby.");
            return;
        }

        proxy.requestSetUsername(nickname);
    }

    /**
     * Handles lobby creation with size constraints.
     */
    public void handleCreateLobby(int size) {
        if (lobbyModel.getCurrentLobby() != null || gameModel != null) {
            view.onError("Action blocked: You must leave your current lobby or game before creating a new one.");
        } else if (size < 2 || size > 5) {
            view.onError("Action blocked: Lobby size must be between 2 and 5 players.");
        } else {
            proxy.requestCreateLobby(size);
        }
    }

    /**
     * Handles joining a lobby by index with dynamic range feedback.
     */
    public void handleJoinLobby(int index) {
        if (lobbyModel.getCurrentLobby() != null || gameModel != null) {
            view.onError("Action blocked: You are already occupied in a lobby or game.");
            return;
        }

        List<LobbyInfo> available = lobbyModel.getAvailableLobbies();
        if (available.isEmpty()) {
            view.onError("Action blocked: No lobbies are currently available.");
            return;
        }

        if (index < 0 || index >= available.size()) {
            view.onError("Action blocked: Invalid index. Please choose a value between 0 and " + (available.size() - 1) + ".");
        } else {
            proxy.requestJoinLobby(available.get(index).lobbyId());
        }
    }

    /**
     * Handles totem selection with checks for nickname availability and totem vacancy.
     */
    public void handleSelectTotem(Totem totem) {
        if (lobbyModel.getMyNickname() == null) {
            view.onError("Action blocked: You must set a nickname before choosing a totem.");
            return;
        }

        LobbyInfo current = lobbyModel.getCurrentLobby();
        if (current == null) {
            view.onError("Action blocked: You must be in a lobby to select a totem.");
            return;
        }

        // Validate if it's a real enum value (useful for Tui parsing)
        if (totem == null) {
            view.onError("Action blocked: Invalid totem color.");
            return;
        }

        // Check availability and identify who took it
        String taker = current.chosenTotems().entrySet().stream()
                .filter(entry -> entry.getValue().equals(totem))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (taker != null) {
            view.onError("Action blocked: Totem " + totem + " is already taken by " + taker + ".");
        } else {
            proxy.requestSelectTotem(totem);
        }
    }

    // -------------------------------------------------------------------------
    // IN-GAME HANDLERS
    // -------------------------------------------------------------------------

    public void handleMoveTotem(char tileID) {
        if (gameModel == null || !isMyTurn()) {
            view.onError("Action blocked: It is not your turn.");
            return;
        }

        if (tileID == 'T') {
            if (gameModel.getCurrentPhase() != PhaseType.ACTION_RESOLUTION) {
                view.onError("Action blocked: You can only return the totem during Action Resolution.");
            } else {
                proxy.moveTotem('T');
            }
        } else {
            if (gameModel.getCurrentPhase() != PhaseType.TOTEM_PLACEMENT) {
                view.onError("Action blocked: You can only place the totem during the Placement phase.");
            } else if (gameModel.getTotemPositions().containsKey(gameModel.getMyNickname())) {
                view.onError("Action blocked: You have already placed your totem this round.");
            } else {
                proxy.moveTotem(tileID);
            }
        }
    }

    public void handleResolveActions(List<String> cardIDs) {
        if (gameModel == null || !isMyTurn() || gameModel.getCurrentPhase() != PhaseType.ACTION_RESOLUTION) {
            view.onError("Action blocked: You cannot resolve actions right now.");
            return;
        }

        // Check if cards actually exist on the board to prevent stale input
        for (String id : cardIDs) {
            boolean exists = gameModel.getUpperRow().contains(id) ||
                    gameModel.getLowerRow().contains(id) ||
                    gameModel.getUpperRowBuildings().contains(id) ||
                    gameModel.getLowerRowBuildings().contains(id);
            if (!exists) {
                view.onError("Action blocked: Card [" + id + "] is no longer on the board.");
                return;
            }
        }

        proxy.resolveActions(cardIDs);
    }

    /**
     * Handles the reconnection request. Validates input and prepares the network
     * layer to recognize the returning player.
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

        // Crucial: we notify the proxy/dispatcher about who is trying to reconnect.
        // This allows ensureGameModel() to work when the first game event arrives.
        proxy.requestReconnect(nickname, gameId);
    }

    protected boolean isMyTurn() {
        return gameModel != null &&
                gameModel.getMyNickname().equals(gameModel.getCurrentPlayer());
    }

    public GameModel getGameModel() { return gameModel; }
    public LobbyModel getLobbyModel() { return lobbyModel; }
}