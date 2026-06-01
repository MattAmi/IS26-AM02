package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds the complete client-side projection of an ongoing game session.
 *
 * <p>Each {@code update*} method corresponds to a specific server notification.
 * The method applies the delta to the local state and then fans out the update
 * to all registered {@link it.polimi.ingsw.am02.client.view.ClientView} observers,
 * which may include both the GUI and the TUI simultaneously.
 *
 * <p>All public methods are {@code synchronized} on the model's own monitor,
 * so views running on different threads can safely read state at any time.
 */
public class GameModel {

    private final String myNickname;
    private String gameId;
    private PhaseType currentPhase;
    private String currentPlayer;
    private Map<String, Totem> totemByPlayer;
    private Era currentEra = Era.I;
    private List<String> turnOrder = new ArrayList<>();
    private List<String> upperRow = new ArrayList<>();
    private List<String> lowerRow = new ArrayList<>();
    private List<String> upperRowBuildings = new ArrayList<>();
    private List<String> lowerRowBuildings = new ArrayList<>();
    private List<OfferTileInfo> offerTiles = new ArrayList<>();
    private int deckRemainingCount;
    private final Map<String, Integer> foodByPlayer = new LinkedHashMap<>();
    private final Map<String, Integer> ppByPlayer = new LinkedHashMap<>();
    private final Map<String, Character> totemPositions = new LinkedHashMap<>();
    private final Map<String, Integer> turnOrderPositions = new LinkedHashMap<>();
    private final Map<String, Integer> remainingUpper = new LinkedHashMap<>();
    private final Map<String, Integer> remainingLower = new LinkedHashMap<>();
    private final Map<String, List<String>> charactersByPlayer = new LinkedHashMap<>();
    private final Map<String, List<String>> buildingsByPlayer = new LinkedHashMap<>();
    private List<String> winners = new ArrayList<>();
    private List<PlayerFinalScore> finalRankings = new ArrayList<>();
    private boolean gameEnded = false;
    private String lastErrorMessage;
    private String lastEventResolved;
    private List<TurnOrderSlotInfo> turnOrderSlots = new ArrayList<>();

    private final List<ClientView> clientViews = new ArrayList<>();

    /**
     * Creates a new, empty game model for the given player.
     *
     * @param myNickname the nickname of the local player
     */
    public GameModel(String myNickname) {
        this.myNickname = myNickname;
    }

    // OBSERVERS

    /**
     * Registers a {@link it.polimi.ingsw.am02.client.view.ClientView} to receive
     * future game updates. No-op if already registered.
     *
     * @param observer the view to add
     */
    public synchronized void addObserver(ClientView observer) {
        if (!clientViews.contains(observer)) {
            clientViews.add(observer);
        }
    }

    /**
     * Removes a previously registered observer.
     *
     * @param clientView the view to remove
     */
    public synchronized void removeObserver(ClientView clientView) { clientViews.remove(clientView); }

    // DOMAIN UPDATES

    /**
     * Applies a game-started notification: stores the game ID, resets the
     * {@code gameEnded} flag, and notifies all views.
     *
     * @param gameID the identifier of the started game
     */
    public synchronized void updateGameStarted(String gameID) {
        this.gameId = gameID;
        this.gameEnded = false;
        clientViews.forEach(o -> o.onGameStarted(gameID));
    }

    /**
     * Applies the initial game-setup snapshot: populates all board and player
     * collections from the server snapshot and notifies all views.
     *
     * @param totemByPlayer map from nickname to assigned totem
     * @param turnOrder     the initial player placement order
     * @param initialFood   starting food per player
     * @param boardSnapshot the full initial board state, or {@code null}
     */
    public synchronized void updateGameSetupCompleted(Map<String, Totem> totemByPlayer,
                                                      List<String> turnOrder,
                                                      Map<String, Integer> initialFood,
                                                      BoardSnapshot boardSnapshot) {
        this.totemByPlayer = new LinkedHashMap<>(totemByPlayer);
        this.turnOrder = new ArrayList<>(turnOrder);

        this.upperRow.clear();
        this.lowerRow.clear();
        this.upperRowBuildings.clear();
        this.lowerRowBuildings.clear();
        this.foodByPlayer.clear();
        this.ppByPlayer.clear();
        this.charactersByPlayer.clear();
        this.buildingsByPlayer.clear();
        this.remainingUpper.clear();
        this.remainingLower.clear();
        this.turnOrderSlots.clear();
        this.currentEra = Era.I;

        this.foodByPlayer.putAll(initialFood);
        initialFood.keySet().forEach(n -> ppByPlayer.put(n, 0));

        if (boardSnapshot != null) {
            this.upperRow = new ArrayList<>(boardSnapshot.upperRowCards());
            this.lowerRow = new ArrayList<>(boardSnapshot.lowerRowCards());
            this.upperRowBuildings = new ArrayList<>(boardSnapshot.upperRowBuildings());
            this.lowerRowBuildings = new ArrayList<>(boardSnapshot.lowerRowBuildings());
            this.offerTiles = new ArrayList<>(boardSnapshot.offerTiles());
            this.deckRemainingCount = boardSnapshot.tribuDeckSize();
            this.turnOrderSlots = new ArrayList<>(boardSnapshot.turnOrderSlots());

            for (OfferTileInfo tile : boardSnapshot.offerTiles()) {
                if (tile.occupantNickname() != null) {
                    remainingUpper.put(tile.occupantNickname(), tile.upperChoosable());
                    remainingLower.put(tile.occupantNickname(), tile.lowerChoosable());
                }
            }
        }

        clientViews.forEach(o -> o.onGameSetupCompleted(turnOrder, initialFood, boardSnapshot));
    }

    /**
     * Applies a phase transition: updates the current phase, optionally the
     * active player and resolution order, re-syncs pick limits when entering
     * {@link it.polimi.ingsw.am02.common.enumerations.PhaseType#ACTION_RESOLUTION},
     * and notifies all views.
     *
     * @param phase           the new game phase
     * @param currentPlayer   the active player's nickname, or {@code null} if unchanged
     * @param resolutionOrder the updated resolution order, or {@code null} if unchanged
     */
    public synchronized void updateCurrentPhase(PhaseType phase, String currentPlayer,
                                                List<String> resolutionOrder) {
        this.currentPhase = phase;
        if (currentPlayer != null) this.currentPlayer = currentPlayer;
        if (resolutionOrder != null) this.turnOrder = new ArrayList<>(resolutionOrder);

        if (phase == PhaseType.ACTION_RESOLUTION) {
            for (OfferTileInfo tile : offerTiles) {
                if (tile.occupantNickname() != null) {
                    remainingUpper.put(tile.occupantNickname(), tile.upperChoosable());
                    remainingLower.put(tile.occupantNickname(), tile.lowerChoosable());
                }
            }
        }

        clientViews.forEach(o -> o.onPhaseChanged(phase, currentPlayer, resolutionOrder));
    }

    /**
     * Applies a current-player change and notifies all views.
     *
     * @param nextPlayer the nickname of the new active player
     */
    public synchronized void updateCurrentPlayer(String nextPlayer) {
        this.currentPlayer = nextPlayer;
        clientViews.forEach(o -> o.onCurrentPlayerChanged(nextPlayer));
    }

    /**
     * Applies a turn order establishment and notifies views.
     *
     * @param turnOrder the new turn order
     */
    public synchronized void updateTurnOrder(List<String> turnOrder) {
        this.turnOrder = new ArrayList<>(turnOrder);
        clientViews.forEach(o -> o.onTurnOrderEstablished(turnOrder));
    }

    /**
     * Applies a totem placement: updates totem positions, offer tiles, turn
     * order slots, and notifies views.
     *
     * @param nickname the player who placed the totem
     * @param tileID   the ID of the tile the totem was placed on
     */
    public synchronized void updateTotemPlaced(String nickname, char tileID) {
        totemPositions.put(nickname, tileID);

        for (int i = 0; i < turnOrderSlots.size(); i++) {
            TurnOrderSlotInfo slot = turnOrderSlots.get(i);
            if (nickname.equals(slot.occupantNickname())) {
                turnOrderSlots.set(i, new TurnOrderSlotInfo(null, slot.foodBonus(), slot.prestigePointsMalus()));
                break;
            }
        }

        this.offerTiles = offerTiles.stream()
                .map(t -> t.tileID() == tileID
                        ? new OfferTileInfo(t.tileID(), t.foodBonus(),
                        t.upperChoosable(), t.lowerChoosable(), nickname)
                        : t)
                .collect(Collectors.toCollection(ArrayList::new));

        clientViews.forEach(o -> o.onTotemPlaced(nickname, tileID));
        clientViews.forEach(o -> o.onOfferTilesUpdated(offerTiles));
    }

    /**
     * Applies a totem return: clears the totem's offer tile position, updates
     * the turn order slot, and notifies views.
     *
     * @param nickname          the player whose totem was returned
     * @param turnOrderPosition the slot index the totem was returned to
     */
    public synchronized void updateTotemReturned(String nickname, int turnOrderPosition) {
        totemPositions.remove(nickname);
        turnOrderPositions.put(nickname, turnOrderPosition);

        int idx = turnOrderPosition;
        if (idx >= 0 && idx < turnOrderSlots.size()) {
            TurnOrderSlotInfo old = turnOrderSlots.get(idx);
            turnOrderSlots.set(idx, new TurnOrderSlotInfo(nickname, old.foodBonus(), old.prestigePointsMalus()));
        }

        this.offerTiles = offerTiles.stream()
                .map(t -> nickname.equals(t.occupantNickname())
                        ? new OfferTileInfo(t.tileID(), t.foodBonus(),
                        t.upperChoosable(), t.lowerChoosable(), null)
                        : t)
                .collect(Collectors.toCollection(ArrayList::new));

        clientViews.forEach(o -> o.onTotemReturned(nickname, turnOrderPosition));
        clientViews.forEach(o -> o.onOfferTilesUpdated(offerTiles));
    }

    /**
     * Applies a board update: replaces both card rows and the deck count,
     * then notifies views.
     *
     * @param newUpperRow        the new upper row card IDs
     * @param newLowerRow        the new lower row card IDs
     * @param deckRemainingCount the updated deck size
     */
    public synchronized void updateBoard(List<String> newUpperRow, List<String> newLowerRow,
                                         int deckRemainingCount) {
        this.upperRow = new ArrayList<>(newUpperRow);
        this.lowerRow = new ArrayList<>(newLowerRow);
        this.deckRemainingCount = deckRemainingCount;
        clientViews.forEach(o -> o.onBoardUpdated(newUpperRow, newLowerRow, deckRemainingCount));
    }

    /**
     * Applies an era change: updates the current era, replaces both building rows,
     * and notifies views.
     *
     * @param newEra               the new era
     * @param newUpperRowBuildings the updated upper row building IDs
     * @param newLowerRowBuildings the updated lower row building IDs
     */
    public synchronized void updateEra(Era newEra, List<String> newUpperRowBuildings,
                                       List<String> newLowerRowBuildings) {
        this.currentEra = newEra;
        this.upperRowBuildings = new ArrayList<>(newUpperRowBuildings);
        this.lowerRowBuildings = new ArrayList<>(newLowerRowBuildings);
        clientViews.forEach(o -> o.onEraChanged(newEra, newUpperRowBuildings, newLowerRowBuildings));
    }

    /**
     * Applies initial pick limits for a player and notifies views.
     *
     * @param nickname      the player
     * @param remainingUpper remaining picks from the upper row
     * @param remainingLower remaining picks from the lower row
     */
    public synchronized void updatePlayerLimitsInitialized(String nickname,
                                                           int remainingUpper,
                                                           int remainingLower) {
        this.remainingUpper.put(nickname, remainingUpper);
        this.remainingLower.put(nickname, remainingLower);
        clientViews.forEach(o -> o.onPlayerLimitsInitialized(nickname, remainingUpper, remainingLower));
    }

    /**
     * Applies updated pick limits for a player and notifies views.
     *
     * @param nickname      the player
     * @param remainingUpper remaining picks from the upper row
     * @param remainingLower remaining picks from the lower row
     */
    public synchronized void updatePlayerLimits(String nickname,
                                                int remainingUpper,
                                                int remainingLower) {
        this.remainingUpper.put(nickname, remainingUpper);
        this.remainingLower.put(nickname, remainingLower);
        clientViews.forEach(o -> o.onPlayerLimitsUpdated(nickname, remainingUpper, remainingLower));
    }

    /**
     * Applies a resource change for a player: updates food or PP maps
     * and notifies views.
     *
     * @param nickname the player
     * @param resource the resource type that changed
     * @param newValue the updated value
     */
    public synchronized void updatePlayerResource(String nickname, ResourceType resource,
                                                  int newValue) {
        if (resource == ResourceType.FOOD) {
            foodByPlayer.put(nickname, newValue);
        } else if (resource == ResourceType.PRESTIGE_POINTS) {
            ppByPlayer.put(nickname, newValue);
        }
        clientViews.forEach(o -> o.onPlayerResourceChanged(nickname, resource, newValue));
    }

    /**
     * Applies a card-taken event: removes the card from the appropriate row,
     * adds it to the player's collection, and notifies views.
     *
     * @param nickname  the player who took the card
     * @param cardID    the card identifier
     * @param cardType  whether it is a character or building card
     * @param sourceRow the row it was taken from
     */
    public synchronized void updateCardTaken(String nickname, String cardID,
                                             CardType cardType, RowPosition sourceRow) {
        List<String> row = (cardType == CardType.BUILDING)
                ? (sourceRow == RowPosition.UPPER ? upperRowBuildings : lowerRowBuildings)
                : (sourceRow == RowPosition.UPPER ? upperRow : lowerRow);
        row.remove(cardID);
        Map<String, List<String>> target =
                (cardType == CardType.BUILDING) ? buildingsByPlayer : charactersByPlayer;
        target.computeIfAbsent(nickname, k -> new ArrayList<>()).add(cardID);
        clientViews.forEach(o -> o.onCardTaken(nickname, cardID, cardType, sourceRow));
    }

    /**
     * Notifies views that the AutoPlayer grace timer has started for the given player.
     *
     * @param nickname the disconnected player for whom the timer is running
     * @param seconds  the duration of the grace period in seconds
     */
    public synchronized void updateAutoPlayerTimerStarted(String nickname, long seconds) {
        clientViews.forEach(o -> o.onAutoPlayerTimerStarted(nickname, seconds));
    }

    /**
     * Notifies views that the auto-player was invoked for the given player.
     *
     * @param nickname the player replaced by the auto-player
     */
    public synchronized void updateAutoPlayerInvoked(String nickname) {
        clientViews.forEach(o -> o.onAutoPlayerInvoked(nickname));
    }

    /**
     * Applies an event resolution: stores the last resolved event name
     * and notifies views.
     *
     * @param eventID   the identifier of the resolved event card
     * @param eventName the display name of the resolved event
     */
    public synchronized void updateEventResolved(String eventID, String eventName) {
        this.lastEventResolved = eventName;
        clientViews.forEach(v -> v.onEventResolved(eventID, eventName));
    }

    /**
     * Applies an extra-turn start: updates pick limits for the player
     * and notifies views.
     *
     * @param nickname      the player receiving the extra turn
     * @param remainingUpper remaining upper-row picks for the extra turn
     * @param remainingLower remaining lower-row picks for the extra turn
     */
    public synchronized void updateExtraTurnStarted(String nickname,
                                                    int remainingUpper,
                                                    int remainingLower) {
        this.remainingUpper.put(nickname, remainingUpper);
        this.remainingLower.put(nickname, remainingLower);
        clientViews.forEach(o -> o.onExtraTurnStarted(nickname, remainingUpper, remainingLower));
    }

    /**
     * Applies an extra-turn end and notifies views.
     *
     * @param nickname the player whose extra turn ended
     */
    public synchronized void updateExtraTurnEnded(String nickname) {
        clientViews.forEach(o -> o.onExtraTurnEnded(nickname));
    }

    /**
     * Applies a game-ended event: stores winners and final rankings,
     * marks the game as ended, and notifies views.
     *
     * @param winners       the list of winning players
     * @param finalRankings the final score ranking
     */
    public synchronized void updateGameEnded(List<String> winners,
                                             List<PlayerFinalScore> finalRankings) {
        this.winners = new ArrayList<>(winners);
        this.finalRankings = new ArrayList<>(finalRankings);
        this.gameEnded = true;
        clientViews.forEach(o -> o.onGameEnded(winners, finalRankings));
    }

    /**
     * Applies a player-disconnected event: stores a diagnostic message
     * and notifies views.
     *
     * @param nickname the disconnected player
     */
    public synchronized void updatePlayerDisconnected(String nickname) {
        this.lastErrorMessage = "Player disconnected: " + nickname;
        clientViews.forEach(o -> o.onPlayerDisconnected(nickname));
    }

    /**
     * Applies a game-aborted event: marks the game as ended and notifies views.
     *
     * @param lastManStanding the nickname of the remaining player, or {@code null}
     */
    public synchronized void updateGameAborted(String lastManStanding) {
        this.gameEnded = true;
        clientViews.forEach(o -> o.onGameAborted(lastManStanding));
    }

    /**
     * Applies a game-recovery-failed event: marks the game as ended
     * and notifies views.
     */
    public synchronized void updateGameRecoveryFailed() {
        this.gameEnded = true;
        clientViews.forEach(o -> o.onGameRecoveryFailed());
    }

    /**
     * Applies a player-reconnected event and notifies views.
     *
     * @param nickname the reconnected player
     */
    public synchronized void updatePlayerReconnected(String nickname) {
        clientViews.forEach(o -> o.onPlayerReconnected(nickname));
    }

    /**
     * Applies an error event: stores the message and notifies views.
     *
     * @param errorMessage the error message from the server
     */
    public synchronized void updateError(String errorMessage) {
        this.lastErrorMessage = errorMessage;
        clientViews.forEach(o -> o.onError(errorMessage));
    }

    // GETTERS

    /** @return the local player's nickname */
    public synchronized String getMyNickname() { return myNickname; }

    /** @return the game's unique identifier, or {@code null} before the game starts */
    public synchronized String getGameId() { return gameId; }

    /** @return the current game phase, or {@code null} before setup completes */
    public synchronized PhaseType getCurrentPhase() { return currentPhase; }

    /** @return the nickname of the currently active player, or {@code null} if not in an action phase */
    public synchronized String getCurrentPlayer() { return currentPlayer; }

    /** @return an immutable copy of the current player order */
    public synchronized List<String> getTurnOrder() { return List.copyOf(turnOrder); }

    /** @return an immutable copy of the upper character/event card row */
    public synchronized List<String> getUpperRow() { return List.copyOf(upperRow); }

    /** @return an immutable copy of the lower character/event card row */
    public synchronized List<String> getLowerRow() { return List.copyOf(lowerRow); }

    /** @return an immutable copy of the upper building market row */
    public synchronized List<String> getUpperRowBuildings() { return List.copyOf(upperRowBuildings); }

    /** @return an immutable copy of the lower building market row */
    public synchronized List<String> getLowerRowBuildings() { return List.copyOf(lowerRowBuildings); }

    /** @return an immutable copy of the current offer tile states */
    public synchronized List<OfferTileInfo> getOfferTiles() { return List.copyOf(offerTiles); }

    /** @return an immutable copy of the food-per-player map */
    public synchronized Map<String, Integer> getFoodByPlayer() { return Map.copyOf(foodByPlayer); }

    /** @return an immutable copy of the prestige-points-per-player map */
    public synchronized Map<String, Integer> getPpByPlayer() { return Map.copyOf(ppByPlayer); }

    /**
     * @return an immutable copy of the totem-position map
     *         (nickname → offer tile ID, or empty if the totem is on the turn-order tile)
     */
    public synchronized Map<String, Character> getTotemPositions() { return Map.copyOf(totemPositions); }

    /** @return an immutable copy of the turn-order-slot-index map (nickname → slot index) */
    public synchronized Map<String, Integer> getTurnOrderPositions() { return Map.copyOf(turnOrderPositions); }

    /** @return an immutable copy of the remaining-upper-picks map (nickname → count) */
    public synchronized Map<String, Integer> getRemainingUpper() { return Map.copyOf(remainingUpper); }

    /** @return an immutable copy of the remaining-lower-picks map (nickname → count) */
    public synchronized Map<String, Integer> getRemainingLower() { return Map.copyOf(remainingLower); }

    /**
     * @return an unmodifiable map from nickname to an immutable list of character card IDs
     *         owned by that player
     */
    public synchronized Map<String, List<String>> getCharactersByPlayer() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        charactersByPlayer.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(copy);
    }

    /**
     * @return an unmodifiable map from nickname to an immutable list of building card IDs
     *         owned by that player
     */
    public synchronized Map<String, List<String>> getBuildingsByPlayer() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        buildingsByPlayer.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(copy);
    }

    /** @return an immutable list of winning player nicknames (empty until the game ends) */
    public synchronized List<String> getWinners() { return List.copyOf(winners); }

    /** @return an immutable list of final-score breakdowns (empty until the game ends) */
    public synchronized List<PlayerFinalScore> getFinalRankings() { return List.copyOf(finalRankings); }

    /** @return the number of cards remaining in the tribe deck */
    public synchronized int getDeckRemainingCount() { return deckRemainingCount; }

    /** @return {@code true} if the game has ended (normally or by abort) */
    public synchronized boolean isGameEnded() { return gameEnded; }

    /** @return the display name of the most recently resolved event, or {@code null} */
    public synchronized String getLastEventResolved() { return lastEventResolved; }

    /** @return the most recent error message received from the server, or {@code null} */
    public synchronized String getLastErrorMessage() { return lastErrorMessage; }

    /**
     * @return {@code true} if a game session is currently active
     *         (game ID is set and the game has not ended)
     */
    public synchronized boolean isInGame() { return gameId != null && !gameEnded; }

    /** @return an immutable copy of the current turn-order tile slot states */
    public synchronized List<TurnOrderSlotInfo> getTurnOrderSlots() { return List.copyOf(turnOrderSlots); }

    /**
     * Overrides the stored game ID.
     * Used during reconnection to update the model before events are replayed.
     *
     * @param gameId the new game identifier
     */
    public synchronized void setGameId(String gameId) { this.gameId = gameId; }

    /**
     * Returns the totem color assigned to the given player.
     *
     * @param nickname the player's nickname
     * @return their {@link it.polimi.ingsw.am02.common.enumerations.Totem},
     *         or {@code null} if the game has not started or the nickname is unknown
     */
    public synchronized Totem getTotem(String nickname) { return totemByPlayer != null ? totemByPlayer.get(nickname) : null; }

    /** @return the current game era (defaults to {@link it.polimi.ingsw.am02.common.enumerations.Era#I}) */
    public synchronized Era getCurrentEra() { return currentEra; }
}