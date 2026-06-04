package it.polimi.ingsw.am02.server.model.tile;

import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.server.model.player.Player;

/**
 * Represents a single offer tile on the offer track.
 * Each tile has a unique identifier, pick limits for the upper and lower card rows,
 * a food reward granted on first action resolution, and a minimum player count.
 *
 * <p>A tile can be occupied by at most one player at a time.
 */
public class OfferTile {

    private final char tileID;
    private final int minPlayers;
    private final int gainedFood;
    private final int numUpperChoosable;
    private final int numLowerChoosable;
    private Player occupyingPlayer;
    private int remainingUpper;
    private int remainingLower;
    private boolean isFoodResolved;

    /**
     * @param tileID             the unique single-character identifier for this tile (e.g. {@code 'A'})
     * @param minPlayers         minimum player count required for this tile to be in play
     * @param gainedFood         food points granted to the player on their first action from this tile
     * @param numUpperChoosable  maximum cards the player may pick from the upper row
     * @param numLowerChoosable  maximum cards the player may pick from the lower row
     */
    public OfferTile(char tileID, int minPlayers, int gainedFood, int numUpperChoosable, int numLowerChoosable) {

        this.tileID = tileID;
        this.minPlayers = minPlayers;
        this.gainedFood = gainedFood;
        this.numUpperChoosable = numUpperChoosable;
        this.numLowerChoosable = numLowerChoosable;
        this.isFoodResolved = false;
    }

    /** @return the tile's single-character identifier */
    public char getTileID() {
        return tileID;
    }

    /** @return the minimum player count for this tile to be active */
    public int getMinPlayers() {
        return minPlayers;
    }

    /** @return the food points granted on first action from this tile */
    public int getGainedFood() {
        return gainedFood;
    }

    /** @return the maximum number of cards the player may pick from the upper row */
    public int getNumUpperChoosable() { return numUpperChoosable; }

    /** @return the maximum number of cards the player may pick from the lower row */
    public int getNumLowerChoosable() {
        return numLowerChoosable;
    }

    /**
     * Places a player on this tile.
     *
     * @param player the player to assign to this tile
     */
    public void acceptPlayer(Player player) { this.occupyingPlayer = player; }

    /**
     * Removes the given player from this tile and resets the food-resolved flag.
     *
     * @param player the player to remove
     */
    public void removePlayer(Player player) {
        this.occupyingPlayer = null;
        this.isFoodResolved = false;
    }

    /** @return {@code true} if a player is currently occupying this tile */
    public boolean isOccupied() {
        return this.occupyingPlayer != null;
    }

    /** @return the player currently occupying this tile, or {@code null} if empty */
    public Player getOccupyingPlayer() { return occupyingPlayer; }

    /**
     * Sets the effective remaining pick counts for the current turn,
     * already capped against the actual number of available cards.
     *
     * @param effectiveUpperChoosable the effective upper-row picks remaining
     * @param effectiveLowerChoosable the effective lower-row picks remaining
     */
    public void setRemainingPicks(int effectiveUpperChoosable, int effectiveLowerChoosable) {
        this.remainingUpper = effectiveUpperChoosable;
        this.remainingLower = effectiveLowerChoosable;
    }

    /** @return the number of upper-row picks still available this turn */
    public int getRemainingUpper() {
        return remainingUpper;
    }

    /** @return the number of lower-row picks still available this turn */
    public int getRemainingLower() {
        return remainingLower;
    }

    /**
     * Decrements the remaining pick counts after a card-selection action.
     *
     * @param upperTaken the number of upper-row cards just taken
     * @param lowerTaken the number of lower-row cards just taken
     */
    public void decrementPicks(int upperTaken, int lowerTaken) {
        remainingUpper -= upperTaken;
        remainingLower -= lowerTaken;
    }

    /**
     * @return {@code true} if both upper and lower remaining picks have reached zero
     */
    public boolean isSatisfied() { return (remainingUpper == 0 && remainingLower == 0); }

    /**
     * Grants the tile's food reward to the player on the first call, then marks the reward
     * as already resolved so it is not granted again in the same turn.
     *
     * @param player the player to receive the food reward
     * @return the amount of food granted, or {@code 0} if the reward was already resolved
     */
    public int resolveFoodOffer(Player player) {
        if (isFoodResolved) {
            return 0;
        }

        player.getTribu().addFoodPoints(gainedFood);
        isFoodResolved = true;

        return gainedFood;
    }

    /**
     * Converts this tile's current state to a DTO for client transmission.
     *
     * @return an {@link OfferTileInfo} snapshot of this tile
     */
    public OfferTileInfo toInfo() {
        String occupantNickname = (occupyingPlayer == null) ? null : occupyingPlayer.getNickname();
        return new OfferTileInfo(tileID, gainedFood, numUpperChoosable, numLowerChoosable, occupantNickname);
    }

}