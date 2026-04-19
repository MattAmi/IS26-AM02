package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.OfferTileInfo;

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

    public OfferTile(char tileID, int minPlayers, int gainedFood, int numUpperChoosable, int numLowerChoosable) {

        this.tileID = tileID;
        this.minPlayers = minPlayers;
        this.gainedFood = gainedFood;
        this.numUpperChoosable = numUpperChoosable;
        this.numLowerChoosable = numLowerChoosable;
        this.isFoodResolved = false;
    }

    public char getTileID() {
        return tileID;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getGainedFood() {
        return gainedFood;
    }

    public int getNumUpperChoosable() { return numUpperChoosable; }

    public int getNumLowerChoosable() {
        return numLowerChoosable;
    }

    public void acceptPlayer(Player player) { this.occupyingPlayer = player; }

    public void removePlayer(Player player) {
        this.occupyingPlayer = null;
        this.isFoodResolved = false;
    }

    public boolean isOccupied() {
        return this.occupyingPlayer != null;
    }

    public Player getOccupyingPlayer() { return occupyingPlayer; }

    public void setRemainingPicks(int effectiveUpperChoosable, int effectiveLowerChoosable) {
        this.remainingUpper = effectiveUpperChoosable;
        this.remainingLower = effectiveLowerChoosable;
    }

    public int getRemainingUpper() {
        return remainingUpper;
    }

    public int getRemainingLower() {
        return remainingLower;
    }

    public void decrementPicks(int upperTaken, int lowerTaken) {
        remainingUpper -= upperTaken;
        remainingLower -= lowerTaken;
    }

    public boolean isSatisfied() { return (remainingUpper == 0 && remainingLower == 0); }

    public int resolveFoodOffer(Player player) {
        if (isFoodResolved) {
            return 0;
        }

        player.getTribu().addFoodPoints(gainedFood);
        isFoodResolved = true;

        return gainedFood;
    }

    public OfferTileInfo toInfo() {
        String occupantNickname = (occupyingPlayer == null) ? null : occupyingPlayer.getNickname();
        return new OfferTileInfo(tileID, gainedFood, numUpperChoosable, numLowerChoosable, occupantNickname);
    }

}