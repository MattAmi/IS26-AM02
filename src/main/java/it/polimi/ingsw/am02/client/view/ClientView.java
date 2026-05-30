package it.polimi.ingsw.am02.client.view;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.List;
import java.util.Map;

/**
 * Observer interface implemented by all client view implementations (TUI, GUI).
 *
 * <p>Both {@link LobbyModel} and {@link GameModel} hold a list of registered
 * {@code ClientView}s and call these methods whenever their state changes.
 * The concrete view is responsible for rendering the change appropriately.
 *
 * <p>All method names follow the {@code on*} convention to signal that they
 * are reactive callbacks, not imperative commands.
 *
 * <p>Group of methods:
 * <ul>
 *   <li><b>Lobby lifecycle</b> — username, available lobbies, current lobby, totem, dissolution</li>
 *   <li><b>Game lifecycle</b> — start, setup, phase changes, turn order</li>
 *   <li><b>Totem and board</b> — totem movements, row updates, era transitions</li>
 *   <li><b>Player state</b>   — pick limits, resources, card acquisitions</li>
 *   <li><b>Events and extra turns</b></li>
 *   <li><b>Game end and errors</b></li>
 *   <li><b>Core view-model binding</b> — game model injection, lobby return</li>
 *   <li><b>Connection</b> — lost/restored, AutoPlayer notifications</li>
 * </ul>
 */
public interface ClientView {

    // Lobby lifecycle
    /**
     * Called when the server responds to a username request.
     *
     * @param username the requested username
     * @param accepted {@code true} if the server accepted it
     * @param reason   the rejection reason, or {@code null} if accepted
     */
    void onUsernameResult(String username, boolean accepted, String reason);

    /**
     * Called when the full list of available lobbies is refreshed.
     *
     * @param lobbies the current list of open lobbies
     */
    void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies);

    /**
     * Called when the state of the lobby this client is in changes.
     *
     * @param lobby the updated lobby info
     */
    void onCurrentLobbyUpdated(LobbyInfo lobby);

    /**
     * Called to prompt the view to display the totem-selection screen or
     * the list of currently available totems.
     */
    void onShowAvailableTotems();

    /** Called when the current lobby has been dissolved by the server. */
    void onLobbyDissolved();

    // Game lifecycle

    /**
     * Called when the server confirms the game has started.
     *
     * @param gameId the unique identifier of the started game
     */
    void onGameStarted(String gameId);

    /**
     * Called once at the start of the game with the full initial state.
     *
     * @param turnOrder    the initial placement order (first player at index 0)
     * @param initialFood  starting food per player (nickname → food)
     * @param board        the full initial board snapshot
     */
    void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board);

    /**
     * Called on every game-phase transition.
     *
     * @param phase           the new game phase
     * @param currentPlayer   the active player's nickname, or {@code null}
     * @param resolutionOrder the full ordered nickname list, or {@code null}
     */
    void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);

    /**
     * Called when the active player changes during totem placement.
     *
     * @param nextPlayer the nickname of the player who must now act
     */
    void onCurrentPlayerChanged(String nextPlayer);

    /**
     * Called at the start of each round with the new placement order.
     *
     * @param turnOrder the ordered list of nicknames for the upcoming round
     */
    void onTurnOrderEstablished(List<String> turnOrder);

    // Totem & board

    /**
     * Called when a player places their totem on an offer tile.
     *
     * @param nickname the placing player's nickname
     * @param tileID   the tile's single-character identifier
     */
    void onTotemPlaced(String nickname, char tileID);

    /**
     * Called when a player returns their totem to the turn-order tile.
     *
     * @param nickname          the player's nickname
     * @param turnOrderPosition the slot index (0-based) they now occupy
     */
    void onTotemReturned(String nickname, int turnOrderPosition);

    /**
     * Called after any change in offer-tile occupancy (placement or return).
     * Provides the full updated offer tile list for convenience.
     *
     * @param offerTiles the current state of all offer tiles
     */
    void onOfferTilesUpdated(List<OfferTileInfo> offerTiles);

    /**
     * Called at the start of a new round with the refreshed card rows.
     *
     * @param newUpperRow        card IDs now in the upper row
     * @param newLowerRow        card IDs now in the lower row
     * @param deckRemainingCount remaining tribe deck size
     */
    void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount);

    /**
     * Called when the game enters a new era and the building market changes.
     *
     * @param newEra               the era now in effect
     * @param newUpperRowBuildings building IDs in the upper building market
     * @param newLowerRowBuildings building IDs in the lower building market
     */
    void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings);

    // Player state

    /**
     * Called at the start of a player's action turn with their initial pick limits.
     *
     * @param nickname       the player
     * @param remainingUpper initial upper-row picks available
     * @param remainingLower initial lower-row picks available
     */
    void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);

    /**
     * Called after a card-selection action with the player's updated remaining picks.
     *
     * @param nickname       the player
     * @param remainingUpper remaining upper-row picks after the last action
     * @param remainingLower remaining lower-row picks after the last action
     */
    void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);

    /**
     * Called whenever a player's resource value changes.
     *
     * @param nickname  the player whose resource changed
     * @param resource  the type of resource
     * @param newValue  the new absolute value
     */
    void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue);

    /**
     * Called when a player acquires a card from the board.
     *
     * @param nickname  the acquiring player's nickname
     * @param cardID    the card identifier
     * @param cardType  {@code CHARACTER} or {@code BUILDING}
     * @param sourceRow the row the card was taken from
     */
    void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);

    // Events & extra turns

    /**
     * Called when an event card has been resolved.
     *
     * @param EventID   the card identifier of the resolved event
     * @param eventName the display name of the event type
     */
    void onEventResolved(String EventID, String eventName);

    /**
     * Called when a player's extra turn begins.
     *
     * @param nickname       the player receiving the extra turn
     * @param remainingUpper upper-row picks available for the extra turn
     * @param remainingLower lower-row picks available for the extra turn
     */
    void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);

    /**
     * Called when a player's extra turn has ended.
     *
     * @param nickname the player whose extra turn concluded
     */
    void onExtraTurnEnded(String nickname);

    // Game end & errors

    /**
     * Called once when the game ends normally.
     *
     * @param winners       the list of winning player nicknames
     * @param finalRankings the full prestige-point breakdown for all players
     */
    void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);

    /**
     * Called when a player disconnects during the game.
     *
     * @param nickname the disconnected player's nickname
     */
    void onPlayerDisconnected(String nickname);

    /**
     * Called when the game is aborted due to forfeit (all but one player disconnected).
     *
     * @param lastManStanding the winning player's nickname, or {@code null}
     */
    void onGameAborted(String lastManStanding);

    /**
     * Called when the server could not complete crash recovery
     * (no players reconnected within the recovery window).
     */
    void onGameRecoveryFailed();

    /**
     * Called when a previously disconnected player reconnects.
     *
     * @param nickname the reconnected player's nickname
     */
    void onPlayerReconnected(String nickname);

    /**
     * Called when the server sends an error message directed at this client only.
     *
     * @param message the human-readable error description
     */
    void onError(String message);

    // Core View-Model binding

    /**
     * Injects the {@link GameModel} into this view after a game starts.
     * Concrete views store the reference and use it to read state between notifications.
     *
     * @param gameModel the game model for the current session
     */
    void setGameModel(GameModel gameModel);

    /**
     * Called when the client has successfully returned to the pre-lobby state.
     * The view should reset any in-game UI and display the lobby screen.
     */
    void onReturnToLobby();

    // Connection

    /**
     * Called when the transport connection to the server is lost.
     * The view should inform the user and disable game controls.
     */
    void onConnectionLost();

    /**
     * Called when a lost connection has been re-established.
     * The view may re-enable controls or show a reconnection prompt.
     */
    void onConnectionRestored();

    /**
     * Called when the AutoPlayer grace timer has started for a disconnected player.
     * The view should inform the other players.
     *
     * @param nickname the disconnected player for whom the timer is running
     */
    void onAutoPlayerTimerStarted(String nickname);

    /**
     * Called immediately before the AutoPlayer acts for a disconnected player.
     *
     * @param nickname the disconnected player being substituted
     */
    void onAutoPlayerInvoked(String nickname);
}