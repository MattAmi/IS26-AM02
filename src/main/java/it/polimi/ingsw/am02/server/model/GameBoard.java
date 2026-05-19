package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;
import it.polimi.ingsw.am02.server.model.exceptions.CardNotFoundException;
import it.polimi.ingsw.am02.server.model.exceptions.EventCardNotTakeableException;
import it.polimi.ingsw.am02.server.model.exceptions.InsufficientFoodException;
import it.polimi.ingsw.am02.server.model.exceptions.PickLimitExceededException;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.GameEventEmitter;

import java.util.*;
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
    private final GameEventEmitter notifier;


    public GameBoard(int numPlayers, GameEventEmitter notifier, Random gameRandom) {
        this.upperRow = new ArrayList<>();
        this.lowerRow = new ArrayList<>();
        this.upperRowBuildings = new ArrayList<>();
        this.lowerRowBuildings = new ArrayList<>();

        this.currentEra = Era.I;
        this.eraChangedFlag = false;

        this.extraTurnPlayer = null;
        this.extraTurnRemainingUpper = 0;
        this.extraTurnRemainingLower = 0;

        this.setUpGameBoard(numPlayers, gameRandom);
        this.setUpInitialRows(numPlayers);
        this.eventObservers = new ArrayList<>();

        this.notifier = Objects.requireNonNull(notifier, "GameEventEmitter must not be null");
    }

    private void setUpGameBoard(int numPlayers, Random gameRandom) {
        this.offerTrack = new OfferTrack(numPlayers);
        this.turnOrderTile = new TurnOrderTile(numPlayers);
        this.tribuDeck = new TribuDeck(numPlayers, gameRandom);
        this.buildingDeck = new BuildingDeck(numPlayers, gameRandom);
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

        // Notify observers that a totem has been placed
        notifier.notifyTotemPlaced(player.getNickname(), tileID);
    }

    public List<Player> getPlayersInResolutionOrder() {
        return offerTrack.getOrderedPlayers();
    }

    public boolean areAllTotemsPlaced() {
        return turnOrderTile.isEmpty();
    }

    public void initializePlayerLimits(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);

        int availableUpper = upperRow.size() + upperRowBuildings.size();
        int availableLower = lowerRow.size() + lowerRowBuildings.size();

        int effectiveUpper = Math.min(currentTile.getNumUpperChoosable(), availableUpper);
        int effectiveLower = Math.min(currentTile.getNumLowerChoosable(), availableLower);

        currentTile.setRemainingPicks(effectiveUpper, effectiveLower);

        // Notify observers that the player's resource limits have been initialized
        notifier.notifyPlayerLimitsInitialized(player.getNickname(), effectiveUpper, effectiveLower);
    }

    private int[] countSelectedByRow(List<String> selectedIDs) {
        int countUpper = 0;
        int countLower = 0;

        for (String cardID : selectedIDs) {
            if (!upperRow.contains(cardID) && !lowerRow.contains(cardID)
                    && !upperRowBuildings.contains(cardID) && !lowerRowBuildings.contains(cardID)) {
                throw new CardNotFoundException(cardID);

            } else if (upperRow.contains(cardID) || upperRowBuildings.contains(cardID)) {
                countUpper++;
            } else if (lowerRow.contains(cardID) || lowerRowBuildings.contains(cardID)) {
                countLower++;
            }
        }

        return new int[]{countUpper, countLower};
    }

    private Map<String, Integer> validateAndComputeCosts(List<String> selectedIDs, Player player) {
        GameRegistry registry = GameRegistry.getInstance();
        Map<String, Integer> buildingCosts = new HashMap<>();
        int totalBuildingCost = 0;

        for (String cardID : selectedIDs) {
            if (registry.isEvent(cardID)) {
                throw new EventCardNotTakeableException(cardID);

            } else if (registry.isBuilding(cardID)) {
                int actualCost = computeActualBuildingCost(cardID, player);
                buildingCosts.put(cardID, actualCost);
                totalBuildingCost += actualCost;
            }
        }

        if (totalBuildingCost > player.getTribu().getFoodPoints()) {
            throw new InsufficientFoodException(totalBuildingCost, player.getTribu().getFoodPoints());
        }

        return buildingCosts;
    }

    private void executeCardAcquisition(List<String> selectedIDs, Player player, Game game,
                                        Map<String, Integer> buildingCosts) {

        GameRegistry registry = GameRegistry.getInstance();

        for (String cardID : selectedIDs) {
            if (registry.isBuilding(cardID)) {
                int actualCost = buildingCosts.get(cardID);

                RowPosition sourceRow;
                if (upperRowBuildings.remove(cardID)) {
                    sourceRow = RowPosition.UPPER;
                } else {
                    lowerRowBuildings.remove(cardID);
                    sourceRow = RowPosition.LOWER;
                }

                notifier.notifyCardTaken(player.getNickname(), cardID, CardType.BUILDING, sourceRow);

                if (actualCost > 0) {
                    player.getTribu().addFoodPoints(-actualCost);
                    notifier.notifyPlayerResourceChanged(
                            player.getNickname(),
                            ResourceType.FOOD,
                            player.getTribu().getFoodPoints(),
                            -actualCost);
                }

                EffectOutcome buildingOutcome = player.getTribu().insertBuilding(cardID, player, game);
                notifier.emitOutcome(buildingOutcome);

            } else {
                RowPosition sourceRow;
                if (upperRow.remove(cardID)) {
                    sourceRow = RowPosition.UPPER;
                } else {
                    lowerRow.remove(cardID);
                    sourceRow = RowPosition.LOWER;
                }
                notifier.notifyCardTaken(player.getNickname(), cardID, CardType.CHARACTER, sourceRow);

                EffectOutcome characterOutcome = player.getTribu().insertCharacter(cardID, player);
                notifier.emitOutcome(characterOutcome);
            }
        }
    }

    private int computeActualBuildingCost(String cardID, Player player) {
        int buildingCost = GameRegistry.getInstance().getBuilding(cardID).getBuildingCost();
        int buildingDiscount = player.getTribu().getBuildingDiscount();

        return Math.max(0, buildingCost - buildingDiscount);
    }

    public void processActionSelection(Player player, List<String> selectedIDs, Game game) {

        OfferTile currentTile = offerTrack.getTileByPlayer(player);

        int[] counts = countSelectedByRow(selectedIDs);

        if (counts[0] > currentTile.getRemainingUpper() || counts[1] > currentTile.getRemainingLower()) {
            throw new PickLimitExceededException(
                    "Upper limit exceeded: " + counts[0] + "/" + currentTile.getRemainingUpper() +
                            ", Lower limit exceeded: " + counts[1] + "/" + currentTile.getRemainingLower()
            );
        }

        Map<String, Integer> buildingCosts = validateAndComputeCosts(selectedIDs, player);

        int foodFromTile = currentTile.resolveFoodOffer(player);
        if (foodFromTile > 0) {
            notifier.notifyPlayerResourceChanged(
                    player.getNickname(),
                    ResourceType.FOOD,
                    player.getTribu().getFoodPoints(),
                    foodFromTile);
        }

        executeCardAcquisition(selectedIDs, player, game, buildingCosts);

        currentTile.decrementPicks(counts[0], counts[1]);

        notifier.notifyPlayerLimitsUpdated(
                player.getNickname(),
                currentTile.getRemainingUpper(),
                currentTile.getRemainingLower());
    }

    public boolean canPlayerFinish(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);

        // Se il giocatore ha già soddisfatto tutti i pick previsti, può terminare il turno.
        if (currentTile.isSatisfied()) {
            return true;
        }

        GameRegistry registry = GameRegistry.getInstance();

        // Controlliamo se ci sono effettivamente dei Personaggi (Characters) nelle righe.
        // Gli Edifici (Buildings) e gli Eventi vengono ignorati per l'obbligo di pesca.
        boolean hasCharacterInUpper = upperRow.stream().anyMatch(registry::isCharacter);
        boolean hasCharacterInLower = lowerRow.stream().anyMatch(registry::isCharacter);

        // L'obbligo sussiste solo se il giocatore ha pick residui E ci sono Personaggi disponibili.
        boolean forcedByUpper = currentTile.getRemainingUpper() > 0 && hasCharacterInUpper;
        boolean forcedByLower = currentTile.getRemainingLower() > 0 && hasCharacterInLower;

        // Se non è forzato né dalla riga superiore né da quella inferiore, può passare.
        return !forcedByUpper && !forcedByLower;
    }

    public void movePlayerToTurnOrder(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);
        currentTile.removePlayer(player);
        int position = turnOrderTile.registerPlayer(player);

        // Notify observers that a totem has been returned to the TurnOrderTile
        notifier.notifyTotemReturned(player.getNickname(), position);
    }

    public void applyTurnOrderRewards(Player player) {
        TurnOrderTile.TurnOrderRewardResult result = turnOrderTile.applyRewards(player);
        String nickname = player.getNickname();
        Tribu tribu = player.getTribu();

        // Notify observers of resource updates (gains and penalties)
        if (result.foodGained() > 0) {
            notifier.notifyPlayerResourceChanged(nickname, ResourceType.FOOD, tribu.getFoodPoints(), result.foodGained());
        }
        if (result.foodPenalty() > 0) {
            notifier.notifyPlayerResourceChanged(nickname, ResourceType.FOOD, tribu.getFoodPoints(), -result.foodPenalty());
        }
        if (result.ppPenalty() > 0) {
            notifier.notifyPlayerResourceChanged(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), -result.ppPenalty());
        }
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
            processAndNotifyEvent(event, players);
        }
    }

    public boolean hasEraChanged() {
        return eraChangedFlag;
    }

    public boolean isTribuDeckEmpty() {
        return tribuDeck.isEmpty();
    }

    public void prepareNewRound(int numPlayers) {
        GameRegistry registry = GameRegistry.getInstance();

        List<String> discardedCards = new ArrayList<>();
        List<String> movedToLowerRow = new ArrayList<>();


        discardedCards.addAll(lowerRow);
        lowerRow.clear();

        for (String cardID : upperRow) {
            lowerRow.add(cardID);
            movedToLowerRow.add(cardID);
        }
        upperRow.clear();
        eraChangedFlag = false;

        for (int i = 0; i < (numPlayers + 4); i++) {
            if (tribuDeck.isEmpty())
                break;
            String cardID = tribuDeck.draw();
            Era cardEra = getCardEra(cardID);

            if (cardEra != currentEra) {
                currentEra = cardEra;
                eraChangedFlag = true;
            }
            upperRow.add(cardID);
        }

        // Notify observers of the board state update, including card movements and deck size
        notifier.notifyBoardUpdated(
                List.copyOf(upperRow),
                List.copyOf(lowerRow),
                discardedCards,
                movedToLowerRow,
                tribuDeck.getRemainingSize()
        );
    }

    public List<Player> getPlayersInPlacementOrder() {
        return turnOrderTile.getOrderedPlayers();
    }

    public void updateRowsForNewEra() {
        List<String> discardedBuildings = new ArrayList<>();

        if (currentEra == Era.III) {
            discardedBuildings.addAll(lowerRowBuildings);
            lowerRowBuildings.clear();
        }

        if (currentEra == Era.II || currentEra == Era.III) {
            lowerRowBuildings.addAll(upperRowBuildings);
            upperRowBuildings.clear();

            List<String> newBuildings = buildingDeck.getBuildingsForEra(currentEra);
            upperRowBuildings.addAll(newBuildings);
        }

        eraChangedFlag = false;

        // Notify observers of the era change and the updated building market
        notifier.notifyEraChanged(
                currentEra,
                List.copyOf(upperRowBuildings),
                List.copyOf(lowerRowBuildings),
                discardedBuildings
        );
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
            processAndNotifyEvent(event, players);
        }
    }

    private void processAndNotifyEvent(EventCard event, List<Player> players) {
        for(EventObserver observer: eventObservers) {
            notifier.emitOutcome(observer.eventStart(event.getType()));
        }

        notifier.notifyEventResolved(event.getID(), event.getType().toString());
        notifier.emitOutcome(event.applyEventEffect(players, eventObservers));

        for(EventObserver observer: eventObservers) {
            notifier.emitOutcome(observer.eventEnd(event.getType()));
        }
    }

    public void initializeExtraPlayerLimits(Player player, int upperPicks, int lowerPicks) {
        this.extraTurnPlayer = player;

        int availableUpper = upperRow.size() + upperRowBuildings.size();
        int availableLower = lowerRow.size() + lowerRowBuildings.size();

        this.extraTurnRemainingUpper = Math.min(upperPicks, availableUpper);
        this.extraTurnRemainingLower = Math.min(lowerPicks, availableLower);

        // Initializes and broadcasts the turn limits for a specific player
        notifier.notifyPlayerLimitsInitialized(
                player.getNickname(),
                extraTurnRemainingUpper,
                extraTurnRemainingLower);
    }

    public void processExtraActionSelection(Player player, List<String> selectedIDs, Game game) {
        int[] counts = countSelectedByRow(selectedIDs);

        if (counts[0] > extraTurnRemainingUpper || counts[1] > extraTurnRemainingLower) {
            throw new IllegalArgumentException(
                    "extra turn: upper=" + counts[0] + "/" + extraTurnRemainingUpper +
                            ", lower=" + counts[1] + "/" + extraTurnRemainingLower
            );
        }

        Map<String, Integer> buildingCosts = validateAndComputeCosts(selectedIDs, player);
        executeCardAcquisition(selectedIDs, player, game, buildingCosts);

        extraTurnRemainingUpper -= counts[0];
        extraTurnRemainingLower -= counts[1];

        notifier.notifyPlayerLimitsUpdated(
                player.getNickname(),
                extraTurnRemainingUpper,
                extraTurnRemainingLower);
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



    // FOR TESTING
    TurnOrderTile getTurnOrderTile() { return turnOrderTile; } // For testing
    List<String> getUpperRow() { return upperRow; } // For testing
    List<String> getLowerRow() { return lowerRow; } // For testing
    List<String> getUpperRowBuildings() { return upperRowBuildings; } // For testing
    List<String> getLowerRowBuildings() { return lowerRowBuildings; } // For testing
    OfferTrack getOfferTrack() { return  offerTrack; } // For testing

    public BoardSnapshot buildSnapshot() {
        List<OfferTileInfo> offerTiles = offerTrack.getTilesInfo();

        List<String> turnOrderPositions = turnOrderTile.getOrderedPlayers().stream()
                .map(Player::getNickname)
                .toList();

        return new BoardSnapshot(
                List.copyOf(upperRow),
                List.copyOf(lowerRow),
                List.copyOf(upperRowBuildings),
                List.copyOf(lowerRowBuildings),
                offerTiles,
                turnOrderTile.toSlotSnapshot(),
                tribuDeck.getRemainingSize()
        );
    }
}