package it.polimi.ingsw.am02.client.model;

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

public class ClientModel {

    // --- Lobby state ---
    private String myNickname;
    private List<LobbyInfo> availableLobbies = new ArrayList<>();
    private LobbyInfo currentLobby;

    // --- Game state ---
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
    private final Map<String, Character> totemPositions = new LinkedHashMap<>(); // nickname -> tileID placed
    private final Map<String, Integer> turnOrderPositions = new LinkedHashMap<>(); // nickname -> position after returning
    private final Map<String, Integer> remainingUpper = new LinkedHashMap<>();
    private final Map<String, Integer> remainingLower = new LinkedHashMap<>();
    private final Map<String, List<String>> charactersByPlayer = new LinkedHashMap<>();
    private final Map<String, List<String>> buildingsByPlayer  = new LinkedHashMap<>();
    private List<String> winners = new ArrayList<>();
    private List<PlayerFinalScore> finalRankings = new ArrayList<>();
    private boolean gameEnded = false;
    private String lastErrorMessage;
    private String lastEventResolved;

    // --- Observers ---
    private final List<ClientModelObserver> observers = new ArrayList<>();

    public void addObserver(ClientModelObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        observers.forEach(ClientModelObserver::update);
    }

    // --- VirtualView ---

    public void apply(Event event) {
        try {
            switch (event) {
                // Lobby events
                case UsernameResultEvent e -> {
                    if (e.isValid()) this.myNickname = e.username();
                }
                case UpdatedLobbiesEvent e -> this.availableLobbies = e.lobbies();
                case UpdatedLobbyEvent e -> this.currentLobby = e.lobby();
                case LobbyDissolvedEvent ignored -> this.currentLobby = null;

                // Game events
                case GameStartedEvent e -> {
                    this.gameId = e.gameID();
                    this.currentLobby = null;
                    this.gameEnded = false;
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
                }
                case PhaseChangedEvent e -> {
                    this.currentPhase = e.phase();
                    if (e.currentPlayer() != null) this.currentPlayer = e.currentPlayer();
                    if (e.resolutionOrder() != null) this.turnOrder = new ArrayList<>(e.resolutionOrder());
                }
                case CurrentPlayerChangedEvent e -> this.currentPlayer = e.nextPlayer();
                case TurnOrderEstablishedEvent e -> this.turnOrder = new ArrayList<>(e.turnOrder());

                case TotemPlacedEvent e -> {
                    totemPositions.put(e.nickname(), e.tileID());
                    this.offerTiles = offerTiles.stream()
                            .map(t -> t.tileID() == e.tileID()
                                    ? new OfferTileInfo(t.tileID(), t.foodBonus(), t.upperChoosable(), t.lowerChoosable(), e.nickname())
                                    : t)
                            .collect(Collectors.toCollection(ArrayList::new));
                }

                case TotemReturnedEvent e -> {
                    totemPositions.remove(e.nickname());
                    turnOrderPositions.put(e.nickname(), e.turnOrderPosition());
                    this.offerTiles = offerTiles.stream()
                            .map(t -> e.nickname().equals(t.occupantNickname())
                                    ? new OfferTileInfo(t.tileID(), t.foodBonus(), t.upperChoosable(), t.lowerChoosable(), null)
                                    : t)
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                case BoardUpdatedEvent e -> {
                    this.upperRow = new ArrayList<>(e.newUpperRow());
                    this.lowerRow = new ArrayList<>(e.newLowerRow());
                    this.deckRemainingCount = e.deckRemainingCount();
                }
                case PlayerLimitsInitializedEvent e -> {
                    remainingUpper.put(e.nickname(), e.remainingUpper());
                    remainingLower.put(e.nickname(), e.remainingLower());
                }
                case PlayerLimitsUpdatedEvent e -> {
                    remainingUpper.put(e.nickname(), e.remainingUpper());
                    remainingLower.put(e.nickname(), e.remainingLower());
                }
                case PlayerResourceChangedEvent e -> {
                    if (e.resource() == ResourceType.FOOD) foodByPlayer.put(e.nickname(), e.newValue());
                    else if (e.resource() == ResourceType.PRESTIGE_POINTS) ppByPlayer.put(e.nickname(), e.newValue());
                }
                case CardTakenEvent e -> {
                    // 1) rimuovi dalla riga del board
                    List<String> row = (e.cardType() == CardType.BUILDING)
                            ? (e.sourceRow() == RowPosition.UPPER ? upperRowBuildings : lowerRowBuildings)
                            : (e.sourceRow() == RowPosition.UPPER ? upperRow : lowerRow);
                    row.remove(e.cardID());

                    // 2) aggiungi alla tribù del player
                    Map<String, List<String>> target =
                            (e.cardType() == CardType.BUILDING) ? buildingsByPlayer : charactersByPlayer;
                    target.computeIfAbsent(e.nickname(), k -> new ArrayList<>()).add(e.cardID());
                }
                case EventResolvedEvent e -> this.lastEventResolved = e.eventName();
                case EraChangedEvent e -> {
                    this.upperRowBuildings = new ArrayList<>(e.newUpperRowBuildings());
                    this.lowerRowBuildings = new ArrayList<>(e.newLowerRowBuildings());
                }
                case ExtraTurnStartedEvent e -> {
                    remainingUpper.put(e.nickname(), e.remainingUpper());
                    remainingLower.put(e.nickname(), e.remainingLower());
                }
                case ExtraTurnEndedEvent ignored -> {}
                case GameEndedEvent e -> {
                    this.winners = new ArrayList<>(e.winners());
                    this.finalRankings = new ArrayList<>(e.finalRankings());
                    this.gameEnded = true;
                }
                case PlayerDisconnectedEvent e -> this.lastErrorMessage = "Player disconnected: " + e.nickname();

                case ErrorEvent e -> this.lastErrorMessage = e.errorMessage();

                default -> {}
            }
        } catch (Exception ex) {
            this.lastErrorMessage = "Internal client error on "
                    + event.getClass().getSimpleName() + ": " + ex.getMessage();
            // Utile durante lo sviluppo: stampa lo stack trace sulla console del client.
            ex.printStackTrace();
        } finally {
            notifyObservers();
        }
    }

    // --- Getters lobby ---
    public String getMyNickname() { return myNickname; }
    public List<LobbyInfo> getAvailableLobbies() { return availableLobbies; }
    public LobbyInfo getCurrentLobby() { return currentLobby; }

    // --- Getters game ---
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
    public Map<String, List<String>> getBuildingsByPlayer()  { return buildingsByPlayer; }
    public boolean isGameEnded() { return gameEnded; }
    public List<String> getWinners() { return winners; }
    public List<PlayerFinalScore> getFinalRankings() { return finalRankings; }
    public String getLastEventResolved() { return lastEventResolved; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public boolean isInGame() { return gameId != null && !gameEnded; }
}