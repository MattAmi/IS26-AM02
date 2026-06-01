package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;
import it.polimi.ingsw.am02.server.model.card.BuildingDeck;
import it.polimi.ingsw.am02.server.model.card.EventCard;
import it.polimi.ingsw.am02.server.model.card.TribuDeck;
import it.polimi.ingsw.am02.server.model.exceptions.CardNotFoundException;
import it.polimi.ingsw.am02.server.model.exceptions.EventCardNotTakeableException;
import it.polimi.ingsw.am02.server.model.exceptions.InsufficientFoodException;
import it.polimi.ingsw.am02.server.model.exceptions.PickLimitExceededException;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.GameEventEmitter;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import it.polimi.ingsw.am02.server.model.tile.OfferTile;
import it.polimi.ingsw.am02.server.model.tile.OfferTrack;
import it.polimi.ingsw.am02.server.model.tile.TurnOrderTile;

import java.util.*;
import java.util.stream.Stream;

/**
 * Manages the shared physical state of the game board: the two card rows (upper / lower),
 * the building market rows, the offer track, the turn-order tile, and the two decks.
 *
 * <p>All mutating operations emit notifications through the injected {@link GameEventEmitter}.
 * {@code GameBoard} is owned by {@link Game} and should not be accessed directly by network layers.
 */
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


    /**
     * Constructs and fully initializes the game board for the given player count.
     * Draws the initial card rows from the freshly built decks.
     *
     * @param numPlayers  the number of players in the game (2–5)
     * @param notifier    the event emitter used to broadcast board changes; must not be {@code null}
     * @param gameRandom  the seeded random source shared with the rest of the game
     * @throws NullPointerException if {@code notifier} is {@code null}
     */
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

    /**
     * Places each player on the turn-order tile in the given order and grants initial food bonuses.
     * Must be called once immediately after board construction.
     *
     * @param orderedPlayers players in randomized starting order (index 0 = first player)
     */
    public void setUpInitialTurnOrder(List<Player> orderedPlayers) {

        for(int i = 0; i < orderedPlayers.size(); i++) {
            Player player = orderedPlayers.get(i);
            turnOrderTile.registerPlayer(player);
            player.getTribu().addFoodPoints(initialFoodBonuses[i]);
        }
    }

    /**
     * Moves a player's totem from the turn-order tile onto the specified offer tile,
     * then fires a {@code totemPlaced} notification.
     *
     * @param player the player performing the move
     * @param tileID the target offer tile identifier
     */
    public void movePlayerToOffer(Player player, char tileID) {
        offerTrack.occupyTile(player, tileID);
        turnOrderTile.removePlayer(player);

        // Notify observers that a totem has been placed
        notifier.notifyTotemPlaced(player.getNickname(), tileID);
    }

    /**
     * @return an ordered list of players currently on the offer track,
     *         sorted by tile ID (ascending), for action-resolution turn order
     */
    public List<Player> getPlayersInResolutionOrder() {
        return offerTrack.getOrderedPlayers();
    }

    /**
     * @return {@code true} if all player totems have been placed on the offer track
     *         (i.e. the turn-order tile is empty)
     */
    public boolean areAllTotemsPlaced() {
        return turnOrderTile.isEmpty();
    }

    /**
     * Initializes the remaining pick counts for the given player based on their offer tile
     * and the number of cards actually available in each row.
     * Fires a {@code playerLimitsInitialized} notification.
     *
     * @param player the player whose limits are being set up
     */
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

    /**
     * Validates and executes a card-selection action for the given player during their turn.
     * Applies food rewards from the offer tile on first call, deducts building costs,
     * transfers cards to the player's tribe, and fires the appropriate notifications.
     *
     * @param player      the player performing the action
     * @param selectedIDs list of card IDs the player wishes to acquire
     * @param game        the current game instance, forwarded to building-effect factories
     * @throws CardNotFoundException       if a selected card is not on the board
     * @throws EventCardNotTakeableException if a selected card is an event card
     * @throws InsufficientFoodException   if the player cannot afford the selected buildings
     * @throws PickLimitExceededException  if the selection exceeds the player's remaining pick limits
     */
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

    /**
     * Determines whether the given player is allowed to end their turn.
     * A player may end if all mandatory picks are satisfied, or if no characters
     * remain available in a row the player is still obliged to pick from.
     *
     * @param player the player requesting to end their turn
     * @return {@code true} if the player may legally end their turn
     */
    public boolean canPlayerFinish(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);

        if (currentTile.isSatisfied()) {
            return true;
        }

        GameRegistry registry = GameRegistry.getInstance();

        boolean hasCharacterInUpper = upperRow.stream().anyMatch(registry::isCharacter);
        boolean hasCharacterInLower = lowerRow.stream().anyMatch(registry::isCharacter);

        boolean forcedByUpper = currentTile.getRemainingUpper() > 0 && hasCharacterInUpper;
        boolean forcedByLower = currentTile.getRemainingLower() > 0 && hasCharacterInLower;

        return !forcedByUpper && !forcedByLower;
    }

    /**
     * Moves a player's totem from their offer tile back to the turn-order tile,
     * claiming the earliest free position. Fires a {@code totemReturned} notification.
     *
     * @param player the player returning their totem
     */
    public void movePlayerToTurnOrder(Player player) {
        OfferTile currentTile = offerTrack.getTileByPlayer(player);
        currentTile.removePlayer(player);
        int position = turnOrderTile.registerPlayer(player);

        // Notify observers that a totem has been returned to the TurnOrderTile
        notifier.notifyTotemReturned(player.getNickname(), position);
    }

    /**
     * Applies end-of-turn food bonuses and penalties defined by the player's
     * position on the turn-order tile. Fires resource-change notifications for each delta.
     *
     * @param player the player whose end-of-turn rewards are being resolved
     */
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

    /** @return the number of players whose totems are currently on the turn-order tile */
    public int getPlayersOnTurnOrderCount() {
        return turnOrderTile.getPlayerCount();
    }

    /**
     * @return {@code true} if there is at least one non-final event card visible in the lower row
     */
    public boolean hasRoundEvents() {
        GameRegistry registry = GameRegistry.getInstance();

        for(String cardID : lowerRow) {
            if(registry.isEvent(cardID) && !registry.getEvent(cardID).isFinal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves all non-final event cards currently in the lower row, in priority order.
     * Each event's outcome is emitted via the notifier.
     *
     * @param players the list of all players (all are affected by events)
     */
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

    /**
     * @return {@code true} if the most recently drawn card belongs to a different era
     *         than the previous one (signals an era transition)
     */
    public boolean hasEraChanged() {
        return eraChangedFlag;
    }


    /**
     * Prepares the board for a new round: discards the lower row, shifts the upper row
     * down, draws new cards from the tribe deck to refill the upper row, and fires a
     * {@code boardUpdated} notification with the full diff.
     *
     * @param numPlayers the number of players (determines how many cards to draw)
     */
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

    /**
     * @return players in the order they should place their totems for the upcoming round,
     *         derived from their current positions on the turn-order tile
     */
    public List<Player> getPlayersInPlacementOrder() {
        return turnOrderTile.getOrderedPlayers();
    }

    /**
     * Updates the building market rows when a new era begins.
     * Moves upper-row buildings to the lower row and draws new buildings for the current era.
     * Discards lower-row buildings when entering Era III.
     * Fires an {@code eraChanged} notification.
     */
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

    /**
     * @return {@code true} if there is at least one final event card in either visible row
     */
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

    /**
     * Resolves all final event cards visible in both rows, in priority order.
     *
     * @param players the list of all players
     */
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

    /**
     * Initializes the pick limits for a player's extra turn, capped by the actual
     * number of cards available in each row. Fires a {@code playerLimitsInitialized} notification.
     *
     * @param player      the player receiving the extra turn
     * @param upperPicks  the maximum number of upper-row picks for the extra turn
     * @param lowerPicks  the maximum number of lower-row picks for the extra turn
     */
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

    /**
     * Validates and executes a card-selection action during a player's extra turn.
     * Behaves like {@link #processActionSelection} but uses the extra-turn pick counters.
     *
     * @param player      the player performing the extra-turn action
     * @param selectedIDs list of card IDs the player wishes to acquire
     * @param game        the current game instance
     * @throws IllegalArgumentException   if the selection exceeds extra-turn pick limits
     * @throws CardNotFoundException      if a selected card is not on the board
     * @throws InsufficientFoodException  if the player cannot afford the selected buildings
     */
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

    /**
     * Resets all extra-turn state after the extra turn has ended.
     */
    public void clearExtraTurn() {
        this.extraTurnPlayer = null;
        this.extraTurnRemainingUpper = 0;
        this.extraTurnRemainingLower = 0;
    }

    /**
     * Grants one bonus food point to the given player if their turn-order position
     * provides a positive food reward, as a side effect of a building effect.
     *
     * @param player the player who may receive the bonus
     */
    public void applyExtraTurnOrderBonus(Player player) {
        int baseBonus = turnOrderTile.getFoodForPlayer(player);

        if (baseBonus > 0) {
            player.getTribu().addFoodPoints(1);
        }
    }

    /**
     * Registers an {@link EventObserver} that will be notified before, during,
     * and after each event resolution.
     *
     * @param effect the observer to attach
     */
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

    /**
     * Builds and returns an immutable snapshot of the current board state,
     * suitable for sending to clients as a DTO.
     *
     * @return a {@link BoardSnapshot} reflecting the current rows, building market,
     *         offer track, turn-order tile, and remaining deck size
     */
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