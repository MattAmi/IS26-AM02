package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.error.ErrorEvent;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;

import java.util.*;
import java.util.stream.Collectors;

public class GameModel {

    private final String myNickname;
    private String gameId;
    private PhaseType currentPhase;
    private String currentPlayer;
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


    public GameModel(String myNickname) {
        this.myNickname = myNickname;
    }

    public void addObserver(ClientView clientView) { clientViews.add(clientView);}
    public void removeObserver(ClientView clientView) { clientViews.remove(clientView); }

    // EVENT DISPATCH

    /**
     * Applies a server-originated event: updates internal state and
     * notifies registered views via the corresponding granular callback.
     */
    public void apply(Event event) {
        try {
            switch (event) {

                // --------- Game lifecycle ---------
                case GameStartedEvent e -> {
                    this.gameId = e.gameID();
                    this.gameEnded = false;
                    clientViews.forEach(o -> o.onGameStarted(e.gameID()));
                }

                case GameSetupCompletedEvent e -> {
                    this.turnOrder = new ArrayList<>(e.turnOrder());

                    //Clean
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

                    this.foodByPlayer.putAll(e.initialFood());
                    e.initialFood().keySet().forEach(n -> ppByPlayer.put(n, 0));
                    BoardSnapshot snap = e.boardSnapshot();
                    if (snap != null) {
                        this.upperRow = new ArrayList<>(snap.upperRowCards());
                        this.lowerRow = new ArrayList<>(snap.lowerRowCards());
                        this.upperRowBuildings = new ArrayList<>(snap.upperRowBuildings());
                        this.lowerRowBuildings = new ArrayList<>(snap.lowerRowBuildings());
                        this.offerTiles = new ArrayList<>(snap.offerTiles());
                        this.deckRemainingCount = snap.tribuDeckSize();
                        this.turnOrderSlots = new ArrayList<>(snap.turnOrderSlots());

                        // --- AGGIUNTA: Popola i limiti iniziali ---
                        for (OfferTileInfo tile : snap.offerTiles()) {
                            if (tile.occupantNickname() != null) {
                                remainingUpper.put(tile.occupantNickname(), tile.upperChoosable());
                                remainingLower.put(tile.occupantNickname(), tile.lowerChoosable());
                            }
                        }
                    }
                    clientViews.forEach(o -> o.onGameSetupCompleted(
                            e.turnOrder(), e.initialFood(), snap));
                }

                case PhaseChangedEvent e -> {
                    this.currentPhase = e.phase();
                    if (e.currentPlayer() != null) this.currentPlayer = e.currentPlayer();
                    if (e.resolutionOrder() != null) this.turnOrder = new ArrayList<>(e.resolutionOrder());

                    // --- AGGIUNTA: Sincronizza i limiti all'inizio della risoluzione ---
                    if (e.phase() == PhaseType.ACTION_RESOLUTION) {
                        for (OfferTileInfo tile : offerTiles) {
                            if (tile.occupantNickname() != null) {
                                remainingUpper.put(tile.occupantNickname(), tile.upperChoosable());
                                remainingLower.put(tile.occupantNickname(), tile.lowerChoosable());
                            }
                        }
                    }

                    clientViews.forEach(o -> o.onPhaseChanged(
                            e.phase(), e.currentPlayer(), e.resolutionOrder()));
                }

                case CurrentPlayerChangedEvent e -> {
                    this.currentPlayer = e.nextPlayer();
                    clientViews.forEach(o -> o.onCurrentPlayerChanged(e.nextPlayer()));
                }

                case TurnOrderEstablishedEvent e -> {
                    this.turnOrder = new ArrayList<>(e.turnOrder());
                    clientViews.forEach(o -> o.onTurnOrderEstablished(e.turnOrder()));
                }

                // --------- Totem & board ---------
                case TotemPlacedEvent e -> {
                    totemPositions.put(e.nickname(), e.tileID());

                    for (int i = 0; i < turnOrderSlots.size(); i++) {
                        TurnOrderSlotInfo slot = turnOrderSlots.get(i);
                        if (e.nickname().equals(slot.occupantNickname())) {
                            turnOrderSlots.set(i, new TurnOrderSlotInfo(null, slot.foodBonus(), slot.prestigePointsMalus()));
                            break;
                        }
                    }

                    this.offerTiles = offerTiles.stream()
                            .map(t -> t.tileID() == e.tileID()
                                    ? new OfferTileInfo(t.tileID(), t.foodBonus(),
                                    t.upperChoosable(), t.lowerChoosable(), e.nickname())
                                    : t)
                            .collect(Collectors.toCollection(ArrayList::new));
                    clientViews.forEach(o -> o.onTotemPlaced(e.nickname(), e.tileID()));
                    clientViews.forEach(o -> o.onOfferTilesUpdated(offerTiles));
                }

                case TotemReturnedEvent e -> {
                    totemPositions.remove(e.nickname());
                    turnOrderPositions.put(e.nickname(), e.turnOrderPosition());
                    // Aggiorna lo slot nella lista (position è 0-based o 1-based? verifica nel tuo server)
                    int idx = e.turnOrderPosition(); // adatta se 1-based: idx = e.turnOrderPosition() - 1
                    if (idx >= 0 && idx < turnOrderSlots.size()) {
                        TurnOrderSlotInfo old = turnOrderSlots.get(idx);
                        turnOrderSlots.set(idx, new TurnOrderSlotInfo(e.nickname(), old.foodBonus(), old.prestigePointsMalus()));
                    }

                    this.offerTiles = offerTiles.stream()
                            .map(t -> e.nickname().equals(t.occupantNickname())
                                    ? new OfferTileInfo(t.tileID(), t.foodBonus(),
                                    t.upperChoosable(), t.lowerChoosable(), null)
                                    : t)
                            .collect(Collectors.toCollection(ArrayList::new));
                    clientViews.forEach(o -> o.onTotemReturned(e.nickname(), e.turnOrderPosition()));
                    clientViews.forEach(o -> o.onOfferTilesUpdated(offerTiles));
                }

                case BoardUpdatedEvent e -> {
                    this.upperRow = new ArrayList<>(e.newUpperRow());
                    this.lowerRow = new ArrayList<>(e.newLowerRow());
                    this.deckRemainingCount = e.deckRemainingCount();
                    clientViews.forEach(o -> o.onBoardUpdated(
                            e.newUpperRow(), e.newLowerRow(), e.deckRemainingCount()));
                }

                case EraChangedEvent e -> {
                    this.upperRowBuildings = new ArrayList<>(e.newUpperRowBuildings());
                    this.lowerRowBuildings = new ArrayList<>(e.newLowerRowBuildings());
                    clientViews.forEach(o -> o.onEraChanged(
                            e.newUpperRowBuildings(), e.newLowerRowBuildings()));
                }

                // --------- Player state ---------
                case PlayerLimitsInitializedEvent e -> {
                    remainingUpper.put(e.nickname(), e.remainingUpper());
                    remainingLower.put(e.nickname(), e.remainingLower());
                    clientViews.forEach(o -> o.onPlayerLimitsInitialized(
                            e.nickname(), e.remainingUpper(), e.remainingLower()));
                }

                case PlayerLimitsUpdatedEvent e -> {
                    remainingUpper.put(e.nickname(), e.remainingUpper());
                    remainingLower.put(e.nickname(), e.remainingLower());
                    clientViews.forEach(o -> o.onPlayerLimitsUpdated(
                            e.nickname(), e.remainingUpper(), e.remainingLower()));
                }

                case PlayerResourceChangedEvent e -> {
                    if (e.resource() == ResourceType.FOOD) {
                        foodByPlayer.put(e.nickname(), e.newValue());
                    } else if (e.resource() == ResourceType.PRESTIGE_POINTS) {
                        ppByPlayer.put(e.nickname(), e.newValue());
                    }
                    clientViews.forEach(o -> o.onPlayerResourceChanged(
                            e.nickname(), e.resource(), e.newValue()));
                }

                case CardTakenEvent e -> {
                    List<String> row = (e.cardType() == CardType.BUILDING)
                            ? (e.sourceRow() == RowPosition.UPPER ? upperRowBuildings : lowerRowBuildings)
                            : (e.sourceRow() == RowPosition.UPPER ? upperRow : lowerRow);
                    row.remove(e.cardID());
                    Map<String, List<String>> target =
                            (e.cardType() == CardType.BUILDING) ? buildingsByPlayer : charactersByPlayer;
                    target.computeIfAbsent(e.nickname(), k -> new ArrayList<>()).add(e.cardID());
                    clientViews.forEach(o -> o.onCardTaken(
                            e.nickname(), e.cardID(), e.cardType(), e.sourceRow()));
                }

                case AutoPlayerTimerStartedEvent e -> {
                    clientViews.forEach(o -> o.onAutoPlayerTimerStarted(e.nickname()));
                }

                case AutoPlayerInvokedEvent e -> {
                    clientViews.forEach(o -> o.onAutoPlayerInvoked(e.nickname()));
                }

                // --------- Events & extra turns ---------
                case EventResolvedEvent e -> {
                    this.lastEventResolved = e.eventName();
                    clientViews.forEach(v -> v.onEventResolved(e.eventID(), e.eventName()));
                }

                case ExtraTurnStartedEvent e -> {
                    remainingUpper.put(e.nickname(), e.remainingUpper());
                    remainingLower.put(e.nickname(), e.remainingLower());
                    clientViews.forEach(o -> o.onExtraTurnStarted(
                            e.nickname(), e.remainingUpper(), e.remainingLower()));
                }

                case ExtraTurnEndedEvent e -> {
                    clientViews.forEach(o -> o.onExtraTurnEnded(e.nickname()));
                }

                // --------- Game end & errors ---------
                case GameEndedEvent e -> {
                    this.winners = new ArrayList<>(e.winners());
                    this.finalRankings = new ArrayList<>(e.finalRankings());
                    this.gameEnded = true;
                    clientViews.forEach(o -> o.onGameEnded(e.winners(), e.finalRankings()));
                }

                case PlayerDisconnectedEvent e -> {
                    this.lastErrorMessage = "Player disconnected: " + e.nickname();
                    clientViews.forEach(o -> o.onPlayerDisconnected(e.nickname()));
                }

                case GameAbortedEvent e -> {
                    this.gameEnded = true;
                    clientViews.forEach(o -> o.onGameAborted(e.lastManStanding()));
                }

                case GameRecoveryFailedEvent e -> {
                    this.gameEnded = true;
                    clientViews.forEach(o -> o.onGameRecoveryFailed());
                }

                case PlayerReconnectedEvent e -> {
                    clientViews.forEach(o -> o.onPlayerReconnected(e.nickname()));
                }

                case ErrorEvent e -> {
                    this.lastErrorMessage = e.errorMessage();
                    clientViews.forEach(o -> o.onError(e.errorMessage()));
                }

                default -> {}
            }
        } catch (Exception ex) {
            String msg = "Internal client error on "
                    + event.getClass().getSimpleName() + ": " + ex.getMessage();
            this.lastErrorMessage = msg;
            ex.printStackTrace();
            clientViews.forEach(o -> o.onError(msg));
        }
    }

    //  GETTERS
    // (tutti i getter rimangono come nella tua versione attuale: servono al
    //  ClientController per recuperare contesto, e alle view per leggere stato
    //  globale che non è strettamente legato all'ultimo evento)

    public String getMyNickname() { return myNickname; }
    public String getGameId() { return gameId; }
    public PhaseType getCurrentPhase() { return currentPhase; }
    public String getCurrentPlayer() { return currentPlayer; }
    public List<String> getTurnOrder() { return Collections.unmodifiableList(turnOrder); }
    public List<String> getUpperRow() { return Collections.unmodifiableList(upperRow); }
    public List<String> getLowerRow() { return Collections.unmodifiableList(lowerRow); }
    public List<String> getUpperRowBuildings() { return Collections.unmodifiableList(upperRowBuildings); }
    public List<String> getLowerRowBuildings() { return Collections.unmodifiableList(lowerRowBuildings); }
    public List<OfferTileInfo> getOfferTiles() { return Collections.unmodifiableList(offerTiles); }
    public Map<String, Integer> getFoodByPlayer() { return Collections.unmodifiableMap(foodByPlayer); }
    public Map<String, Integer> getPpByPlayer() { return Collections.unmodifiableMap(ppByPlayer); }
    public Map<String, Character> getTotemPositions() { return Collections.unmodifiableMap(totemPositions); }
    public Map<String, Integer> getTurnOrderPositions() { return Collections.unmodifiableMap(turnOrderPositions); }
    public Map<String, Integer> getRemainingUpper() { return Collections.unmodifiableMap(remainingUpper); }
    public Map<String, Integer> getRemainingLower() { return Collections.unmodifiableMap(remainingLower); }
    public Map<String, List<String>> getCharactersByPlayer() { return Collections.unmodifiableMap(charactersByPlayer); }
    public Map<String, List<String>> getBuildingsByPlayer() { return Collections.unmodifiableMap(buildingsByPlayer); }
    public List<String> getWinners() { return Collections.unmodifiableList(winners); }
    public List<PlayerFinalScore> getFinalRankings() { return Collections.unmodifiableList(finalRankings); }
    public int getDeckRemainingCount() { return deckRemainingCount; }
    public boolean isGameEnded() { return gameEnded; }
    public String getLastEventResolved() { return lastEventResolved; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public boolean isInGame() { return gameId != null && !gameEnded; }
    public List<TurnOrderSlotInfo> getTurnOrderSlots() { return Collections.unmodifiableList(turnOrderSlots); }
    public void setGameId(String gameId) { this.gameId = gameId; }

}