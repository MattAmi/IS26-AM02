package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
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
                    this.foodByPlayer.putAll(e.initialFood());
                    e.initialFood().keySet().forEach(n -> ppByPlayer.put(n, 0));
                    BoardSnapshot snap = e.boardSnapshot();
                    if (snap != null) {
                        this.upperRow = new ArrayList<>(snap.upperRowCards());
                        this.lowerRow = new ArrayList<>(snap.lowerRowCards());
                        this.upperRowBuildings = new ArrayList<>(snap.upperRowBuildings());
                        this.lowerRowBuildings = new ArrayList<>(snap.lowerRowBuildings());
                        this.offerTiles = new ArrayList<>(snap.offerTiles());
                    }
                    clientViews.forEach(o -> o.onGameSetupCompleted(
                            e.turnOrder(), e.initialFood(), snap));
                }

                case PhaseChangedEvent e -> {
                    this.currentPhase = e.phase();
                    if (e.currentPlayer() != null) this.currentPlayer = e.currentPlayer();
                    if (e.resolutionOrder() != null) this.turnOrder = new ArrayList<>(e.resolutionOrder());
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

                // --------- Events & extra turns ---------
                case EventResolvedEvent e -> {
                    this.lastEventResolved = e.eventName();
                    clientViews.forEach(o -> o.onEventResolved(e.eventName()));
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
    public List<String> getTurnOrder() { return turnOrder; }
    public List<String> getUpperRow() { return upperRow; }
    public List<String> getLowerRow() { return lowerRow; }
    public List<OfferTileInfo> getOfferTiles() { return offerTiles; }
    public List<String> getUpperRowBuildings() { return upperRowBuildings; }
    public List<String> getLowerRowBuildings() { return lowerRowBuildings; }
    public int getDeckRemainingCount() { return deckRemainingCount; }
    public Map<String, Integer> getFoodByPlayer() { return foodByPlayer; }
    public Map<String, Integer> getPpByPlayer() { return ppByPlayer; }
    public Map<String, Character> getTotemPositions() { return totemPositions; }
    public Map<String, Integer> getTurnOrderPositions() { return turnOrderPositions; }
    public Map<String, Integer> getRemainingUpper() { return remainingUpper; }
    public Map<String, Integer> getRemainingLower() { return remainingLower; }
    public Map<String, List<String>> getCharactersByPlayer() { return charactersByPlayer; }
    public Map<String, List<String>> getBuildingsByPlayer() { return buildingsByPlayer; }
    public boolean isGameEnded() { return gameEnded; }
    public List<String> getWinners() { return winners; }
    public List<PlayerFinalScore> getFinalRankings() { return finalRankings; }
    public String getLastEventResolved() { return lastEventResolved; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public boolean isInGame() { return gameId != null && !gameEnded; }
}