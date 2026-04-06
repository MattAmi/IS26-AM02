package it.polimi.ingsw.am02.model;

import java.util.Collection;
import java.util.List;

public class GameBoard {

    public GameBoard(int numPlayers) {
    }

    public TurnOrderTile getTurnOrderTile() {
        return null;
    }

    public void setUpInitialTurnOrder(List<Player> orderedPlayers) {
    }

    public void movePlayerToOffer(Player player, char tileID) {
    }

    public boolean areAllTotemsPlaced() {
    }

    public List<Player> getPlayersInResolutionOrder() {

    }

    public void initializePlayerLimits(Player currentPlayer) {
    }

    public void processActionSelection(Player player, List<String> selectedIDs) {
    }

    public void movePlayerToTurnOrder(Player player) {
    }

    public boolean canPlayerFinish(Player player) {
    }

    public void applyTurnOrderRewards(Player player) {
    }

    public int getPlayersOnTurnOrderCount() {
    }

    public boolean hasRoundEvents() {
    }

    public void resolveRoundEvents(Collection<Player> values) {
    }

    public void prepareNewRound(int numPlayers) {
    }

    public boolean hasEraChanged() {
    }

    public boolean isTribuDeckEmpty() {
    }

    public boolean hasFinalEvents() {
    }

    public void updateRowsForNewEra() {
    }

    public void resolveFinalEvents(Collection<Player> values) {
    }

    public List<Player> getPlayersInPlacementOrder() {
    }

    public void initializeExtraPlayerLimits(Player playerByNickname, int extraTurnUpperPicks, int extraTurnLowerPicks) {
    }

    public void processExtraActionSelection(Player player, List<String> selectedIDs) {
    }
}