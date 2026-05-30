package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;

import java.util.*;

/**
 * Server-side snapshot of the game state, maintained by {@link GameController}
 * as a passive accumulation of {@code GameObserver} delta-notifications.
 * Read exclusively by {@link AutoPlayer}; never transmitted to clients.
 *
 * <p>Only the fields that AutoPlayer actually needs are tracked.
 * Card metadata is always queried live from {@code GameRegistry},
 * which is a read-only singleton — no duplication occurs here.
 *
 * <p>All writes happen under {@code GameController}'s monitor,
 * so no additional synchronization is needed.
 */
final class ServerGameSnapshot {

    //Phase / turn
    private PhaseType currentPhase;
    private String currentPlayerNickname;
    private boolean extraTurnMode = false;

    // Tribe-card rows
    private final List<String> upperRow = new ArrayList<>();
    private final List<String> lowerRow = new ArrayList<>();

    // Offer track
    private final Map<Character, String> occupantByTile = new LinkedHashMap<>();

    private final Map<String, Integer> remainingUpper = new HashMap<>();
    private final Map<String, Integer> remainingLower = new HashMap<>();

    // Getters — package-private, used only by AutoPlayer

    /** @return the current game phase, or {@code null} before setup completes */
    PhaseType currentPhase() { return currentPhase; }

    /** @return the nickname of the player whose turn it currently is */
    String    currentPlayerNickname() { return currentPlayerNickname; }

    /** @return an immutable copy of the current upper card row */
    List<String> upperRow() { return List.copyOf(upperRow); }

    /** @return an immutable copy of the current lower card row */
    List<String> lowerRow() { return List.copyOf(lowerRow); }

    /**
     * @param nick the player's nickname
     * @return the remaining upper-row picks for that player, or 0 if unknown
     */
    int remainingUpperOf(String nick) { return remainingUpper.getOrDefault(nick, 0); }

    /**
     * @param nick the player's nickname
     * @return the remaining lower-row picks for that player, or 0 if unknown
     */
    int remainingLowerOf(String nick) { return remainingLower.getOrDefault(nick, 0); }

    /**
     * @return a list of offer tile IDs that currently have no occupant,
     *         in insertion order (tile registration order)
     */
    List<Character> freeOfferTiles() {
        List<Character> free = new ArrayList<>();
        for (Map.Entry<Character, String> entry : occupantByTile.entrySet()) {
            if (entry.getValue() == null) {
                free.add(entry.getKey());
            }
        }
        return free;
    }

    // Extra turn
    /**
     * Sets or clears extra-turn mode.
     *
     * @param active {@code true} to enter extra-turn mode, {@code false} to exit
     */
    void setExtraTurnMode(boolean active) { this.extraTurnMode = active; }

    /** @return {@code true} if the game is currently in an extra-turn phase */
    boolean isExtraTurnMode() { return extraTurnMode; }


    // Mutators — called exclusively by GameController's GameObserver callbacks
    /**
     * Initializes the snapshot from the single {@link BoardSnapshot} emitted
     * at the end of {@code SetUpState}. After this call, every subsequent
     * delta keeps the snapshot current without further full-state reads.
     */
    void applySetup(BoardSnapshot board) {
        upperRow.clear();
        upperRow.addAll(board.upperRowCards());

        lowerRow.clear();
        lowerRow.addAll(board.lowerRowCards());

        // Initialize every offer tile as free, then mark occupied ones.
        // Adapt field names to your actual OfferTileInfo record.
        occupantByTile.clear();
        for (OfferTileInfo tile : board.offerTiles()) {
            occupantByTile.put(tile.tileID(), tile.occupantNickname());
        }

    }

    /**
     * Updates the current phase and, optionally, the active player nickname.
     *
     * @param phase         the new phase
     * @param currentPlayer the new active player, or {@code null} to leave unchanged
     */
    void setPhase(PhaseType phase, String currentPlayer) {
        this.currentPhase = phase;
        if (currentPlayer != null) {
            this.currentPlayerNickname = currentPlayer;
        }
    }

    /**
     * Updates the active player without changing the phase.
     *
     * @param nickname the new active player's nickname
     */
    void setCurrentPlayer(String nickname) {
        this.currentPlayerNickname = nickname;
    }

    /**
     * Replaces both card rows with the values from a {@code BoardUpdated} notification.
     *
     * @param newUpperRow the new upper row card IDs
     * @param newLowerRow the new lower row card IDs
     */
    void applyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow) {
        upperRow.clear();
        upperRow.addAll(newUpperRow);
        lowerRow.clear();
        lowerRow.addAll(newLowerRow);
    }

    /**
     * Removes a card from whichever row it currently occupies.
     *
     * @param cardId the card ID just taken by a player
     */
    void applyCardTaken(String cardId) {
        upperRow.remove(cardId);
        lowerRow.remove(cardId);
    }

    /**
     * Marks an offer tile as occupied by the given player.
     *
     * @param nickname the player who just placed their totem
     * @param tileId   the tile they placed it on
     */
    void applyTotemPlaced(String nickname, char tileId) {
        occupantByTile.put(tileId, nickname);
    }

    /**
     * Clears the offer-tile occupant for the given player (totem returned).
     *
     * @param nickname the player whose totem was returned
     */
    void applyTotemReturned(String nickname) {
        occupantByTile.replaceAll((tile, occupant) ->
                nickname.equals(occupant) ? null : occupant);
    }

    /**
     * Updates the remaining pick counts for a player.
     *
     * @param nickname the player's nickname
     * @param upper    remaining upper-row picks
     * @param lower    remaining lower-row picks
     */
    void setPlayerLimits(String nickname, int upper, int lower) {
        remainingUpper.put(nickname, upper);
        remainingLower.put(nickname, lower);
    }

}