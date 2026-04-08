package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.Era;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class GameBoard {

    private final List<String> upperRow;
    private final List<String> upperRowBuildings;
    private final List<String> lowerRow;
    private final List<String> lowerRowBuildings;
    private TurnOrderTile turnOrderTile;
    private OfferTrack offerTrack;
    private TribuDeck tribuDeck;
    private BuildingDeck buildingDeck;
    private Era currentEra;
    private boolean eraChangedFlag;
    private final int[] initialFoodBonuses = {2, 3, 3, 4, 4};
    private Player extraTurnPlayer;
    private int extraTurnRemainingUpper;
    private int extraTurnRemainingLower;
    private final List<EventObserver> eventObservers;


    public GameBoard(int numPlayers) {
        this.upperRow = new ArrayList<>();
        this.lowerRow = new ArrayList<>();
        this.upperRowBuildings = new ArrayList<>();
        this.lowerRowBuildings = new ArrayList<>();

        this.currentEra = Era.I;
        this.eraChangedFlag = false;

        this.extraTurnPlayer = null;
        this.extraTurnRemainingUpper = 0;
        this.extraTurnRemainingLower = 0;

        this.setUpGameBoard(numPlayers);
        this.setUpInitialRows(numPlayers);
        this.eventObservers = new ArrayList<>();
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
        } else {
            return registry.getBuilding(cardID).getEra();
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

    public void processActionSelection(Player player, List<String> selectedIDs, Game game) {
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
            } else if (lowerRow.contains(cardID)) {
                countLower++;
            }
        }

        if (countUpper > currentTile.getRemainingUpper() || countLower > currentTile.getRemainingLower()) {
            throw new IllegalArgumentException("Selection exceeds allowed pick limits."); // TO DO
        }

        GameRegistry registry = GameRegistry.getInstance();

        for(String cardID : selectedIDs) {
            if(registry.isEvent(cardID)) {
                throw new IllegalArgumentException("Event cards cannot be taken: " + cardID); // TO DO
            } else if(registry.isBuilding(cardID)) {
                int actualBuildingCost = computeActualBuildingCost(cardID, player);

                if(player.getTribu().getFoodPoints() < actualBuildingCost) {
                    throw new IllegalArgumentException("Insufficient food to purchase building: " + cardID); // TO DO
                }
            }
        }

        for(String cardID : selectedIDs) {
            if(registry.isBuilding(cardID)) {
                int actualBuildingCost = computeActualBuildingCost(cardID, player);

                player.getTribu().addFoodPoints(-actualBuildingCost);

                player.getTribu().insertBuilding(cardID, game);

                if(upperRowBuildings.contains(cardID)) {
                    upperRowBuildings.remove(cardID);
                } else {
                    lowerRowBuildings.remove(cardID);
                }

            } else {
                player.getTribu().insertCharacter(cardID);
                if(upperRow.contains(cardID)) {
                    upperRow.remove(cardID);
                } else {
                    lowerRow.remove(cardID);
                }
            }
        }

        currentTile.decrementPicks(countUpper, countLower);

    }

    private int computeActualBuildingCost(String cardID, Player player) {
        int buildingCost = GameRegistry.getInstance().getBuilding(cardID).getBuildingCost();
        int buildingDiscount = player.getTribu().getBuildingDiscount();
        return Math.max(0, buildingCost - buildingDiscount);
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

    public boolean hasRoundEvents() {
        GameRegistry registry = GameRegistry.getInstance();
        for(String cardID : lowerRow) {
            if(registry.isEvent(cardID) && !registry.getEvent(cardID).isFinal()) {
                return true;
            }
        }
        return false;
    }

    public void resolveRoundEvents(List<Player> players) {

        GameRegistry registry = GameRegistry.getInstance();

        List<EventCard> sortedEvents = lowerRow.stream()
                .filter(registry::isEvent)
                .map(registry::getEvent)
                .filter(event -> !event.isFinal())
                .sorted(Comparator.comparingInt(EventCard::getPriority))
                .toList();

        for (EventCard event : sortedEvents) {
            for(EventObserver observer: eventObservers)
                observer.EventStart(event.getType());
            event.applyEventEffect(players, eventObservers);
            for(EventObserver observer: eventObservers)
                observer.EventEnd(event.getType());
        }
    }

    public boolean hasEraChanged() {
        return eraChangedFlag;
    }

    public boolean isTribuDeckEmpty() {
        return tribuDeck.isEmpty();
    }

    public void prepareNewRound(int numPlayers) {
        lowerRow.clear();
        lowerRow.addAll(upperRow);
        upperRow.clear();
        eraChangedFlag = false;

        for (int i = 0; i < (numPlayers + 4); i++) {
            String cardID = tribuDeck.draw();
            Era cardEra = getCardEra(cardID);

            if (cardEra != currentEra) {
                currentEra = cardEra;
                eraChangedFlag = true;
            }
            upperRow.add(cardID);
        }
    }

    public List<Player> getPlayersInPlacementOrder() {
        return turnOrderTile.getOrderedPlayers();
    }

    public void updateRowsForNewEra() {
        if(currentEra == Era.III)
            lowerRowBuildings.clear();

        if(currentEra == Era.II || currentEra == Era.III) {
            lowerRowBuildings.addAll(upperRowBuildings);
            upperRowBuildings.clear();

            List<String> newBuildings = buildingDeck.getBuildingsForEra(currentEra);
            upperRowBuildings.addAll(newBuildings);
        }

        eraChangedFlag = false;
    }

    public boolean hasFinalEvents() {
        GameRegistry registry = GameRegistry.getInstance();

        for (String cardID : upperRow) {
            if (registry.isEvent(cardID) && registry.getEvent(cardID).isFinal()) {
                return true;
            }
        }

        for (String cardID : lowerRow) {
            if (registry.isEvent(cardID) && registry.getEvent(cardID).isFinal()) {
                return true;
            }
        }
        return false;
    }

    public void resolveFinalEvents(List<Player> players) {
        GameRegistry registry = GameRegistry.getInstance();

        Stream<String> allVisibleIds = Stream.concat(upperRow.stream(), lowerRow.stream());
        List<EventCard> sortedFinalEvents = allVisibleIds
                .filter(registry::isEvent)
                .map(registry::getEvent)
                .filter(EventCard::isFinal)
                .sorted(Comparator.comparingInt(EventCard::getPriority))
                .toList();

        for (EventCard event : sortedFinalEvents) {
            for(EventObserver observer: eventObservers)
                observer.EventStart(event.getType());
            event.applyEventEffect(players, eventObservers);
            for(EventObserver observer: eventObservers)
                observer.EventEnd(event.getType());
        }
    }

    public void initializeExtraPlayerLimits(Player player, int upperPicks, int lowerPicks) {
        this.extraTurnPlayer =  player;
        this.extraTurnRemainingUpper = Math.min(upperPicks, upperRow.size());
        this.extraTurnRemainingLower = Math.min(lowerPicks, lowerRow.size());
    }

    public void processExtraActionSelection(Player player, List<String> selectedIDs, Game game) {
        int countUpper = 0;
        int countLower = 0;

        for (String cardID : selectedIDs) {
            if (!upperRow.contains(cardID) && !lowerRow.contains(cardID)
                    && !upperRowBuildings.contains(cardID) && !lowerRowBuildings.contains(cardID)) {
                throw new IllegalArgumentException("Card ID not found in any row: " + cardID); // TO DO
            } else if (upperRow.contains(cardID) || upperRowBuildings.contains(cardID)) {
                countUpper++;
            } else if (lowerRow.contains(cardID) || lowerRowBuildings.contains(cardID)) {
                countLower++;
            }
        }

        if (countUpper > extraTurnRemainingUpper || countLower > extraTurnRemainingLower) {
            throw new IllegalArgumentException("Extra turn limits exceeded"); // TODO
        }

        GameRegistry registry = GameRegistry.getInstance();

        for(String cardID : selectedIDs) {
            if(registry.isEvent(cardID)) {
                throw new IllegalArgumentException("Event cards cannot be taken: " + cardID); // TO DO
            } else if(registry.isBuilding(cardID)) {
                int actualBuildingCost = computeActualBuildingCost(cardID, player);

                if(player.getTribu().getFoodPoints() < actualBuildingCost) {
                    throw new IllegalArgumentException("Insufficient food to purchase building: " + cardID); // TODO
                }
            }
        }

        for(String cardID : selectedIDs) {
            if(registry.isBuilding(cardID)) {
                int actualBuildingCost = computeActualBuildingCost(cardID, player);

                player.getTribu().addFoodPoints(-actualBuildingCost);

                player.getTribu().insertBuilding(cardID, game);

                if(upperRowBuildings.contains(cardID)) {
                    upperRowBuildings.remove(cardID);
                } else {
                    lowerRowBuildings.remove(cardID);
                }

            } else {
                player.getTribu().insertCharacter(cardID);
                if(upperRow.contains(cardID)) {
                    upperRow.remove(cardID);
                } else {
                    lowerRow.remove(cardID);
                }
            }
        }

        extraTurnRemainingUpper -= countUpper;
        extraTurnRemainingLower -= countLower;
    }

    public void clearExtraTurn() {
        this.extraTurnPlayer = null;
        this.extraTurnRemainingUpper = 0;
        this.extraTurnRemainingLower = 0;
    }

    public void applyExtraTurnOrderBonus(Player player) {
        int baseBonus = turnOrderTile.getFoodForPlayer(player);

        if (baseBonus > 0) {
            player.getTribu().addFoodPoints(1);
        }
    }

    public void attachEventObserver(EventObserver effect) {
        eventObservers.add(effect);
    }


}