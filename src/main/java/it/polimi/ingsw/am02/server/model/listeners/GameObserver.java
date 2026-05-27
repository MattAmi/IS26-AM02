package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.List;
import java.util.Map;

/**
 * Observer that receives all game-level notifications produced by {@link GameNotifier}.
 * Implemented by the server-side controller layer to relay events to connected clients.
 *
 * <p>Each method corresponds to a distinct game event. All parameters are immutable
 * snapshots or value types safe to pass across thread boundaries.
 */
public interface GameObserver {

    /**
     * Fired once at the start of the game, after setup is complete.
     *
     * @param totemByPlayer  mapping from each player's nickname to their assigned totem
     * @param turnOrder      the initial randomized placement order (first player at index 0)
     * @param initialFood    mapping from each player's nickname to their starting food points
     * @param boardSnapshot  a full snapshot of the initial board state
     */
    void onGameSetupCompleted(Map<String, Totem> totemByPlayer, List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot);

    /**
     * Fired when the game transitions to a new phase (no current player involved).
     *
     * @param phase the new phase
     */
    void onPhaseChanged(PhaseType phase);

    /**
     * Fired when the game transitions to a new phase with an active player.
     *
     * @param phase         the new phase
     * @param currentPlayer the nickname of the player whose turn it is
     */
    void onPhaseChanged(PhaseType phase, String currentPlayer);

    /**
     * Fired when the game transitions to a new phase with a full resolution order.
     *
     * @param phase           the new phase
     * @param currentPlayer   the nickname of the first player to act
     * @param resolutionOrder the ordered list of nicknames for this phase
     */
    void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);

    /**
     * Fired during totem placement when the active player changes.
     *
     * @param nextPlayer the nickname of the player who must now place their totem
     */
    void onCurrentPlayerChanged(String nextPlayer);

    /**
     * Fired at the start of a new round, after the placement order is determined.
     *
     * @param turnOrder the ordered list of nicknames in placement order for this round
     */
    void onTurnOrderEstablished(List<String> turnOrder);

    /**
     * Fired when the game transitions to a new era and the building market is updated.
     *
     * @param newEra               the era now in effect
     * @param newUpperRowBuildings building card IDs now in the upper building market row
     * @param newLowerRowBuildings building card IDs now in the lower building market row
     * @param discardedBuildings   building card IDs removed from play (Era III lower-row discard)
     */
    void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings);


    /**
     * Fired at the start of each new round, describing the full card-row diff.
     *
     * @param newUpperRow       card IDs now in the upper row
     * @param newLowerRow       card IDs now in the lower row
     * @param discardedCards    card IDs removed from play (previous lower row)
     * @param movedToLowerRow   card IDs that shifted from the upper to the lower row
     * @param deckRemainingCount number of cards still in the tribe deck
     */
    void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount);

    /**
     * Fired each time a player acquires a card from the board.
     *
     * @param nickname  the acquiring player's nickname
     * @param cardID    the ID of the card taken
     * @param cardType  whether the card is a {@code CHARACTER} or a {@code BUILDING}
     * @param sourceRow the row the card was taken from ({@code UPPER} or {@code LOWER})
     */
    void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);


    /**
     * Fired when a player places their totem on an offer tile.
     *
     * @param nickname the placing player's nickname
     * @param tileID   the identifier of the occupied tile
     */
    void onTotemPlaced(String nickname, char tileID);

    /**
     * Fired when a player returns their totem to the turn-order tile.
     *
     * @param nickname          the player's nickname
     * @param turnOrderPosition the slot index (0-based) the player now occupies
     */
    void onTotemReturned(String nickname, int turnOrderPosition);

    /**
     * Fired when a player's pick limits are set at the start of their action phase.
     *
     * @param nickname        the player's nickname
     * @param remainingUpper  effective upper-row picks for this turn
     * @param remainingLower  effective lower-row picks for this turn
     */
    void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);

    /**
     * Fired after a player takes cards, reflecting the updated remaining pick counts.
     *
     * @param nickname        the player's nickname
     * @param remainingUpper  remaining upper-row picks after the last action
     * @param remainingLower  remaining lower-row picks after the last action
     */
    void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);

    /**
     * Fired whenever a player's resource value changes (food, prestige, shaman stars, etc.).
     *
     * @param nickname  the player whose resource changed
     * @param resource  the type of resource that changed
     * @param newValue  the new absolute value of the resource after the change
     * @param delta     the signed amount of the change (positive = gain, negative = loss)
     */
    void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta);

    /**
     * Fired when an event card has been resolved.
     *
     * @param eventID   the card ID of the resolved event
     * @param eventName the string name of the event type
     */
    void onEventResolved(String eventID, String eventName);

    /**
     * Fired when a player's extra turn begins.
     *
     * @param nickname        the player receiving the extra turn
     * @param remainingUpper  upper-row picks available during the extra turn
     * @param remainingLower  lower-row picks available during the extra turn
     */
    void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);

    /**
     * Fired when a player's extra turn ends.
     *
     * @param nickname the player whose extra turn has concluded
     */
    void onExtraTurnEnded(String nickname);

    /**
     * Fired once when the game ends, after all scoring is complete.
     *
     * @param winners        list of nicknames of the winner(s) (multiple in case of a tie)
     * @param finalRankings  full final-score breakdown for all players, in no guaranteed order
     */
    void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);
}