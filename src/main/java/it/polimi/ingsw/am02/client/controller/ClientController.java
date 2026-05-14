package it.polimi.ingsw.am02.client.controller;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
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
     * Provides instant UI feedback before executing network operations in the background.
     */
    protected void performReturnToLobby() {

        if (gameModel != null) {
            gameModel.removeObserver(view);
            gameModel = null;
        }
        view.onReturnToLobby();
        new Thread(() -> {
            try {
                proxy.disconnect();
                boolean isConnected = false;
                int attempts = 0;
                while (!isConnected && attempts < 5) {
                    try {
                        Thread.sleep(1000); // Give the socket time to breathe
                        proxy.connect();
                        isConnected = true;
                    } catch (Exception e) {
                        attempts++;
                        System.err.println("[Network] Reconnect attempt " + attempts + " failed: " + e.getMessage());
                        if (attempts >= 5) throw e; // Give up after 5 failures
                    }
                }
                lobbyModel.addObserver(view);

            } catch (Exception e) {
                e.printStackTrace();
                view.onError("Return to lobby failed: The server is unreachable. Please check your connection.");
            }
        }, "ReturnToLobby-Worker").start();
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
     * Validates and forwards a totem-move request.
     *
     * <p>Client-side checks performed (in order):
     * <ol>
     *   <li>A game is in progress and has not yet ended.</li>
     *   <li>The current phase permits totem movement
     *       ({@code TOTEM_PLACEMENT} or {@code ACTION_RESOLUTION}).</li>
     *   <li>It is the caller's turn ({@code currentPlayer} matches own nickname).</li>
     *   <li>For {@code 'T'}: the phase is {@code ACTION_RESOLUTION}
     *       (return is not allowed during placement).</li>
     *   <li>For any other tile during {@code ACTION_RESOLUTION}: blocked entirely,
     *       since the only valid move in that phase is returning via {@code 'T'}.</li>
     *   <li>For any other tile during {@code TOTEM_PLACEMENT}: the tile exists,
     *       is not already occupied, and the caller has not already placed this round.</li>
     * </ol>
     *
     * <p>All checks rely on state that is stable between server notifications:
     * the local {@link GameModel} is updated exclusively by server events,
     * so reads here are consistent with the last acknowledged server state.
     * Turn-order and phase enforcement remain authoritative on the server.
     *
     * @param tileID the identifier of the target offer tile,
     *               or {@code 'T'} to return the totem to the turn-order tile
     */
    public void handleMoveTotem(char tileID) {
        if (gameModel == null) {
            view.onError("Action blocked: No game in progress.");
            return;
        }
        if (gameModel.isGameEnded()) {
            view.onError("Action blocked: The game has already ended.");
            return;
        }

        PhaseType phase = gameModel.getCurrentPhase();

        // moveTotem is only meaningful in these two phases
        if (phase != PhaseType.TOTEM_PLACEMENT && phase != PhaseType.ACTION_RESOLUTION) {
            view.onError("Action blocked: You cannot move your totem in the current phase ("
                    + phase + ").");
            return;
        }

        // Turn check: only the active player may issue this command
        if (!myNickname().equals(gameModel.getCurrentPlayer())) {
            view.onError("Action blocked: It is not your turn.");
            return;
        }

        if (tileID == 'T') {
            // Returning the totem is only valid during ACTION_RESOLUTION
            if (phase != PhaseType.ACTION_RESOLUTION) {
                view.onError("Action blocked: You can only return your totem during action resolution.");
                return;
            }
            // 'T' is always structurally valid — no further check needed
            proxy.moveTotem('T');
            return;
        }

        // Any tile other than 'T' during ACTION_RESOLUTION is meaningless:
        // the only valid move in that phase is returning via 'T'
        if (phase == PhaseType.ACTION_RESOLUTION) {
            view.onError("Action blocked: During action resolution your totem must return "
                    + "to the turn-order tile. Use 'move T'.");
            return;
        }

        // From here on: TOTEM_PLACEMENT phase, tileID != 'T'

        // Tile existence check: offer tiles are fixed at setup and never change
        OfferTileInfo target = gameModel.getOfferTiles().stream()
                .filter(t -> t.tileID() == tileID)
                .findFirst()
                .orElse(null);
        if (target == null) {
            view.onError("Action blocked: Tile '" + tileID + "' does not exist.");
            return;
        }

        // Occupancy check: stable within a placement phase, updated by onTotemPlaced
        if (target.occupantNickname() != null) {
            view.onError("Action blocked: Tile '" + tileID + "' is already occupied by "
                    + target.occupantNickname() + ".");
            return;
        }

        // Each player places their totem exactly once per round during TOTEM_PLACEMENT
        if (gameModel.getTotemPositions().containsKey(myNickname())) {
            view.onError("Action blocked: You have already placed your totem this round.");
            return;
        }

        proxy.moveTotem(tileID);
    }

    /**
     * Validates and forwards a resolve-actions request.
     *
     * <p>Client-side checks performed (in order):
     * <ol>
     *   <li>A game is in progress and has not yet ended.</li>
     *   <li>The current phase is {@code ACTION_RESOLUTION}.</li>
     *   <li>It is the caller's turn ({@code currentPlayer} matches own nickname).</li>
     *   <li>The selection is non-null and non-empty.</li>
     *   <li>No duplicate card IDs are present in the selection.</li>
     *   <li>Every selected card is currently present on the board
     *       (upper/lower row, characters or buildings).</li>
     *   <li>The number of upper-row picks does not exceed the caller's
     *       remaining upper-row allowance.</li>
     *   <li>The number of lower-row picks does not exceed the caller's
     *       remaining lower-row allowance.</li>
     * </ol>
     *
     * <p>Selecting fewer cards than the maximum allowed is always valid:
     * the server accepts partial picks. Whether mandatory character picks
     * are satisfied is enforced exclusively server-side, as the client has
     * no access to card-type metadata ({@code GameRegistry} is server-only).
     * Pick-limit and phase enforcement remain authoritative on the server.
     *
     * @param cardIDs the identifiers of the cards the player wishes to take;
     *                must be non-null, non-empty, and free of duplicates
     */
    public void handleResolveActions(List<String> cardIDs) {
        if (gameModel == null) {
            view.onError("Action blocked: No game in progress.");
            return;
        }
        if (gameModel.isGameEnded()) {
            view.onError("Action blocked: The game has already ended.");
            return;
        }

        // resolveActions is only valid during ACTION_RESOLUTION
        if (gameModel.getCurrentPhase() != PhaseType.ACTION_RESOLUTION) {
            view.onError("Action blocked: You can only resolve actions during the action "
                    + "resolution phase.");
            return;
        }

        // Turn check: only the active player may issue this command
        if (!myNickname().equals(gameModel.getCurrentPlayer())) {
            view.onError("Action blocked: It is not your turn.");
            return;
        }

        if (cardIDs == null || cardIDs.isEmpty()) {
            view.onError("Action blocked: No cards selected. Select at least one card "
                    + "from the board, or use 'move T' to end your turn.");
            return;
        }

        // Duplicate check: each card can only be taken once per action
        long distinctCount = cardIDs.stream().distinct().count();
        if (distinctCount < cardIDs.size()) {
            view.onError("Action blocked: Duplicate card IDs in selection.");
            return;
        }

        // Card presence and row attribution:
        // board contents are stable within a turn — updated only by onCardTaken/onBoardUpdated
        String nick = myNickname();
        int upperPicks = 0;
        int lowerPicks = 0;

        for (String id : cardIDs) {
            boolean inUpperChar     = gameModel.getUpperRow().contains(id);
            boolean inUpperBuilding = gameModel.getUpperRowBuildings().contains(id);
            boolean inLowerChar     = gameModel.getLowerRow().contains(id);
            boolean inLowerBuilding = gameModel.getLowerRowBuildings().contains(id);

            if (!inUpperChar && !inUpperBuilding && !inLowerChar && !inLowerBuilding) {
                view.onError("Action blocked: Card [" + id + "] is not available on the board. "
                        + "It may have already been taken or it does not exist.");
                return;
            }

            if (inUpperChar || inUpperBuilding) upperPicks++;
            else lowerPicks++;  // inLowerChar || inLowerBuilding, mutually exclusive with upper
        }

        // Pick-limit check: the client knows the remaining allowance from onPlayerLimitsUpdated.
        // Whether mandatory character picks are satisfied is enforced server-side only,
        // since the client cannot distinguish character cards from building cards by ID alone.
        int allowedUpper = gameModel.getRemainingUpper().getOrDefault(nick, 0);
        int allowedLower = gameModel.getRemainingLower().getOrDefault(nick, 0);

        if (upperPicks > allowedUpper) {
            int excess = upperPicks - allowedUpper;
            view.onError("Action blocked: Upper-row pick limit exceeded ("
                    + upperPicks + " selected, " + allowedUpper + " remaining). "
                    + "Remove " + excess + " card(s) from your upper-row selection.");
            return;
        }
        if (lowerPicks > allowedLower) {
            int excess = lowerPicks - allowedLower;
            view.onError("Action blocked: Lower-row pick limit exceeded ("
                    + lowerPicks + " selected, " + allowedLower + " remaining). "
                    + "Remove " + excess + " card(s) from your lower-row selection.");
            return;
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

    protected String myNickname() {
        return gameModel != null ? gameModel.getMyNickname() : lobbyModel.getMyNickname();
    }

}