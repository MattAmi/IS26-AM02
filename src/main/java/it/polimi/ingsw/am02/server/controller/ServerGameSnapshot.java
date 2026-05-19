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
 * so no additional synchronisation is needed.
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
    PhaseType currentPhase() { return currentPhase; }
    String    currentPlayerNickname() { return currentPlayerNickname; }
    List<String> upperRow() { return List.copyOf(upperRow); }
    List<String> lowerRow() { return List.copyOf(lowerRow); }
    int remainingUpperOf(String nick) { return remainingUpper.getOrDefault(nick, 0); }
    int remainingLowerOf(String nick) { return remainingLower.getOrDefault(nick, 0); }

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
    void setExtraTurnMode(boolean active) { this.extraTurnMode = active; }
    boolean isExtraTurnMode() { return extraTurnMode; }


    // Mutators — called exclusively by GameController's GameObserver callbacks
    /**
     * Initialises the snapshot from the single {@link BoardSnapshot} emitted
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

    void setPhase(PhaseType phase, String currentPlayer) {
        this.currentPhase = phase;
        if (currentPlayer != null) {
            this.currentPlayerNickname = currentPlayer;
        }
    }

    void setCurrentPlayer(String nickname) {
        this.currentPlayerNickname = nickname;
    }

    void applyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow) {
        upperRow.clear();
        upperRow.addAll(newUpperRow);
        lowerRow.clear();
        lowerRow.addAll(newLowerRow);
    }

    void applyCardTaken(String cardId) {
        upperRow.remove(cardId);
        lowerRow.remove(cardId);
    }

    void applyTotemPlaced(String nickname, char tileId) {
        occupantByTile.put(tileId, nickname);
    }

    void applyTotemReturned(String nickname) {
        occupantByTile.replaceAll((tile, occupant) ->
                nickname.equals(occupant) ? null : occupant);
    }

    void setPlayerLimits(String nickname, int upper, int lower) {
        remainingUpper.put(nickname, upper);
        remainingLower.put(nickname, lower);
    }

}