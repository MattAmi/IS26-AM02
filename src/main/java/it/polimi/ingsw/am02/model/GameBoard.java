package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.Era;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GameBoard {

    private final List<String> upperRow;
    private final List<String> upperRowBuildings;
    private final List<String> lowerRow;
    private final List<String> lowerRowBuildings;
    private TurnOrderTile turnOrderTile;
    private OfferTrack offerTrack;
    private TribuDeck tribuDeck;
    private BuildingDeck buildingDeck;
    private int availableFoodPoints;
    private int availablePrestigePoints;
    private Era currentEra;
    private boolean eraChangedFlag;
    private int[] initialFoodBonuses = {2, 3, 3, 4, 4};


    public GameBoard(int numPlayers) {
        this.upperRow = new ArrayList<>();
        this.lowerRow = new ArrayList<>();
        this.upperRowBuildings = new ArrayList<>();
        this.lowerRowBuildings = new ArrayList<>();

        this.availableFoodPoints = INITIAL_FOOD_POINTS;
        this.availablePrestigePoints = INITIAL_PRESTIGE_POINTS;
        this.currentEra = Era.I;
        this.eraChangedFlag = false;

        this.setUpGameBoard(numPlayers);
        this.setUpInitialRows(numPlayers);
    }

    private void setUpGameBoard(int numPlayers) {
        GameRegistry registry = GameRegistry.getInstance();

        this.offerTrack = new OfferTrack(numPlayers);
        this.turnOrderTile = new TurnOrderTile(numPlayers);
        this.tribuDeck = new TribuDeck(numPlayers);
        this.buildingDeck = new BuildingDeck(numPlayers);
    }

    private void setUpInitialRows(int numPlayers) {
        GameRegistry registry = GameRegistry.getInstance();

        while (lowerRow.size() < (numPlayers + 1) && !tribuDeck.isEmpty()) {
            String cardID = tribuDeck.draw();
            checkAndUpdateEra(cardID);
            if (registry.isEvent(cardID)) {
                this.upperRow.add(cardID);
            } else {
                this.lowerRow.add(cardID);
            }
        }

        while (upperRow.size() < (numPlayers + 4) && !tribuDeck.isEmpty()) {
            String cardID = tribuDeck.draw();
            checkAndUpdateEra(cardID);
            this.upperRow.add(cardID);
        }

        List<String> buildingsI = buildingDeck.getBuildingsForEra(Era.I);
        if (buildingsI != null) {
            upperRowBuildings.addAll(buildingsI);
        }
    }

    private Era getCardEra(String cardID) {
        GameRegistry registry = GameRegistry.getInstance();

        if(registry.isCharacter(cardID)) {
            return registry.getCharacter(cardID).getEra();
        } else if (registry.isEvent(cardID)) {
            return registry.getEvent(cardID).getEra();
        }
    }

    private void checkAndUpdateEra(String cardID) {
        Era cardEra = getCardEra(cardID);
        if (cardEra != currentEra) {
            currentEra = cardEra;
            eraChangedFlag = true;
        }
    }

    public void setUpInitialTurnOrder(List<Player> orderedPlayers) {
        for(int i = 0; i < orderedPlayers.size(); i++) {
            Player player = orderedPlayers.get(i);
            turnOrderTile.registerPlayer(player);
            player.getTribu().addFoodPoints(initialFoodBonuses[i]);
        }
    }

    public void movePlayerToOffer(Player player, char tileID) {
        offerTrack.occupyTile(player, tileID);
        turnOrderTile.removePlayer(player);
    }

    public List<Player> getPlayersInResolutionOrder() {
        return offerTrack.getOrderedPlayers();
    }

    public boolean areAllTotemsPlaced() {
        return turnOrderTile.isEmpty();
    }

    public void initializePlayerLimits(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);

        int effectiveUpperChoosable = Math.min(currentTile.getNumUpperChoosable(), upperRow.size());
        int effectiveLowerChoosable = Math.min(currentTile.getNumLowerChoosable(), lowerRow.size());

        currentTile.setRemainingPicks(effectiveUpperChoosable, effectiveLowerChoosable);
    }

    public void processActionSelection(Player player, List<String> selectedIDs) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);

        currentTile.resolveFoodOffer(player);

        int countUpper = 0;
        int countLower = 0;

        for (String cardID : selectedIDs) {
            if (!upperRow.contains(cardID) && !lowerRow.contains(cardID)
                    && !upperRowBuildings.contains(cardID) && !lowerRowBuildings.contains(cardID)) {
                throw new IllegalArgumentException("Card ID not found in any row: " + cardID); // TO DO
            } else if (upperRow.contains(cardID)) {
                countUpper++;
            } else if (lowerRow.contains(cardID))
                countLower++;
        }

        if (countUpper > currentTile.getRemainingUpper() || countLower > currentTile.getRemainingLower()) {
            throw new IllegalArgumentException("Selection exceeds allowed pick limits."); // TO DO
        }

        GameRegistry registry = GameRegistry.getInstance();

        for(String cardID : selectedIDs) {
            if(registry.isEvent(cardID)) {
                throw new IllegalArgumentException("Event cards cannot be taken: " + cardID); // TO DO
            } else if(registry.isBuilding(cardID)) {
                int buildingCost = registry.getBuilding(cardID).getBuildingCost();
                int buildingDiscount = player.getTribu().getBuildingDiscount();
                int actualCost = Math.max(0, buildingCost - buildingDiscount);

                if(player.getTribu().getFoodDiscount() < actualCost) {
                    throw new IllegalArgumentException("Insufficient food to purchase building: " + cardID); // TO DO
                }
            }
        }

        for(String cardID : selectedIDs) {
            if(registry.isBuilding(cardID)) {
                int buildingCost = registry.getBuilding(cardID).getBuildingCost();
                int buildingDiscount = player.getTribu().getBuildingDiscount();
                int actualCost = Math.max(0, buildingCost - buildingDiscount);

                player.getTribu().addFoodPoints(-actualCost);
                if(upperRowBuildings.contains(cardID)) {
                    upperRowBuildings.remove(cardID);
                } else
                    lowerRowBuildings.remove(cardID);

            } else {
                player.getTribu().insertCharacter(cardID);
                if(upperRow.contains(cardID)) {
                    upperRow.remove(cardID);
                } else
                    lowerRow.remove(cardID);
            }
        }

    }

    public boolean canPlayerFinish(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);
        return currentTile.isSatisfied();
    }

    public void movePlayerToTurnOrder(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);
        currentTile.removePlayer(player);
        turnOrderTile.registerPlayer(player);
    }

    public void applyTurnOrderRewards(Player player) {
        turnOrderTile.applyRewards(player);
    }

    public int getPlayersOnTurnOrderCount() {
        return turnOrderTile.getPlayerCount();
    }

    public boolean hasRoundEvents() {}

    public void resolveRoundEvents(Collection<Player> players) {}

    public boolean hasEraChanged() {
        return eraChangedFlag;
    }

    public boolean isTribuDeckEmpty() {
        return tribuDeck.isEmpty();
    }

    public void prepareNewRound(int numPlayers) {}

    public List<Player> getPlayersInPlacementOrder() {
        return turnOrderTile.getOrderedPlayers();
    }

    public void updateRowsForNewEra() {}

    public boolean hasFinalEvents() {}

    public void resolveFinalEvents(Collection<Player> players) {}

    public void initializeExtraPlayerLimits(Player player, int upperPicks, int lowerPicks) {}

    public void processExtraActionSelection(Player player, List<String> selectedIDs) {}
}