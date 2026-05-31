package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.GameBoard;

import java.util.List;
import java.util.Map;

/**
 * Internal interface through which {@link GameBoard} and {@link Game} fire
 * game-level notifications. Implemented by {@link GameNotifier}, which
 * forwards each call to all registered {@link GameObserver}s.
 *
 * <p>This interface separates the emission API (used by model classes) from the
 * subscription API ({@link GameObserverRegistry}), following the Interface Segregation Principle.
 */
public interface GameEventEmitter {

    //Outcome
    /**
     * Iterates over the {@link EffectOutcome}'s resource deltas and fires
     * {@link #notifyPlayerResourceChanged} for each one.
     * No-op if the outcome is {@code null} or empty.
     *
     * @param outcome the aggregated effect outcome to broadcast
     */
    void emitOutcome(EffectOutcome outcome);

    //Setup
    /**
     * Notifies observers that game setup has completed and the first round is about to start.
     *
     * @param totemByPlayer  mapping from nickname to assigned totem
     * @param turnOrder      the initial randomized placement order
     * @param initialFood    mapping from nickname to starting food points
     * @param boardSnapshot  full snapshot of the initial board state
     */
    void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer, List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot);

    // TotemPlacement
    /**
     * Notifies observers that a player has placed their totem on an offer tile.
     *
     * @param nickname the placing player's nickname
     * @param tileID   the identifier of the occupied tile
     */
    void notifyTotemPlaced(String nickname, char tileID);

    /**
     * Notifies observers that the active player has changed during totem placement.
     *
     * @param nextPlayer the nickname of the player who must now act
     */
    void notifyCurrentPlayerChanged(String nextPlayer);

    /**
     * Notifies observers of the effective pick limits for the current player at turn start.
     *
     * @param nickname        the player's nickname
     * @param remainingUpper  effective upper-row picks
     * @param remainingLower  effective lower-row picks
     */
    void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);

    // ActionResolution
    /**
     * Notifies observers that a player has taken a card from the board.
     *
     * @param nickname  the acquiring player's nickname
     * @param cardID    the ID of the card taken
     * @param cardType  {@code CHARACTER} or {@code BUILDING}
     * @param sourceRow the row the card came from
     */
    void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);

    /**
     * Notifies observers of updated remaining pick counts after a card-selection action.
     *
     * @param nickname        the player's nickname
     * @param remainingUpper  remaining upper-row picks
     * @param remainingLower  remaining lower-row picks
     */
    void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);

    /**
     * Notifies observers that a player's totem has returned to the turn-order tile.
     *
     * @param nickname          the player's nickname
     * @param turnOrderPosition the slot index (0-based) assigned to the player
     */
    void notifyTotemReturned(String nickname, int turnOrderPosition);

    /**
     * Notifies observers that a player's resource value has changed.
     *
     * @param nickname  the player whose resource changed
     * @param resource  the type of resource
     * @param newValue  the new absolute value
     * @param delta     the signed change amount
     */
    void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta);

    // NewRound
    /**
     * Notifies observers of the full card-row diff at the start of a new round.
     *
     * @param newUpperRow        card IDs now in the upper row
     * @param newLowerRow        card IDs now in the lower row
     * @param discardedCards     card IDs removed from play
     * @param movedToLowerRow    card IDs shifted from upper to lower
     * @param deckRemainingCount remaining tribe-deck size
     */
    void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount);

    /**
     * Notifies observers of the placement order for the upcoming round.
     *
     * @param turnOrder ordered list of nicknames in placement order
     */
    void notifyTurnOrderEstablished(List<String> turnOrder);

    // NewEra
    /**
     * Notifies observers that the game has entered a new era and the building market has changed.
     *
     * @param newEra               the era now in effect
     * @param newUpperRowBuildings building IDs in the upper building market
     * @param newLowerRowBuildings building IDs in the lower building market
     * @param discardedBuildings   building IDs removed from play
     */
    void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings);

    // Phase changes
    /**
     * Notifies observers of a phase transition (no current player).
     *
     * @param phase the new phase
     */
    void notifyPhaseChanged(PhaseType phase);

    /**
     * Notifies observers of a phase transition with an active player.
     *
     * @param phase         the new phase
     * @param currentPlayer the nickname of the active player
     */
    void notifyPhaseChanged(PhaseType phase, String currentPlayer);

    /**
     * Notifies observers of a phase transition with a full resolution order.
     *
     * @param phase           the new phase
     * @param currentPlayer   the first active player's nickname
     * @param resolutionOrder the complete ordered list of nicknames
     */
    void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);

    // EventResolution
    /**
     * Notifies observers that an event card has been resolved.
     *
     * @param eventID   the card ID of the resolved event
     * @param eventName the string name of the event type
     */
    void notifyEventResolved(String eventID, String eventName);

    // ExtraTurn
    /**
     * Notifies observers that a player's extra turn has begun.
     *
     * @param nickname        the player's nickname
     * @param remainingUpper  upper-row picks for the extra turn
     * @param remainingLower  lower-row picks for the extra turn
     */
    void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);

    /**
     * Notifies observers that a player's extra turn has ended.
     *
     * @param nickname the player's nickname
     */
    void notifyExtraTurnEnded(String nickname);

    // FinaScoring
    /**
     * Notifies observers that the game has ended and final scores are available.
     *
     * @param winners        list of winner nicknames (multiple in case of a tie)
     * @param finalRankings  complete scoring breakdown for all players
     */
    void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);
}
