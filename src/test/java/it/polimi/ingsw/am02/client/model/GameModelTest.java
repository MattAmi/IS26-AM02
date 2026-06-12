package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the client-side {@link GameModel}, verifying that every server update —
 * setup, phase/turn changes, totem placement, board and era refreshes, pick
 * limits, resources, card draws, events, extra turns and game end — mutates the
 * local state correctly and is forwarded to the registered {@link ClientView}.
 */
class GameModelTest {

    private GameModel gameModel;
    private ClientView mockView;
    private final String myNickname = "me";

    @BeforeEach
    void setUp() {
        gameModel = new GameModel(myNickname);
        mockView = Mockito.mock(ClientView.class);
    }

    /** Verifies observers are registered once and stop receiving updates after removal. */
    @Test
    void testAddRemoveObserver() {
        gameModel.addObserver(mockView);
        gameModel.updateError("error");
        verify(mockView).onError("error");

        gameModel.addObserver(mockView); // Test adding same observer
        gameModel.updateError("error2");
        verify(mockView, times(1)).onError("error2");

        gameModel.removeObserver(mockView);
        gameModel.updateError("error3");
        verify(mockView, never()).onError("error3");
    }

    /** Verifies game start stores the game id, clears the ended flag and notifies the view. */
    @Test
    void testUpdateGameStarted() {
        gameModel.addObserver(mockView);
        gameModel.updateGameStarted("game123");
        assertEquals("game123", gameModel.getGameId());
        assertFalse(gameModel.isGameEnded());
        verify(mockView).onGameStarted("game123");
    }

    /** Verifies setup completion populates totems, turn order, food, rows and limits (and tolerates a null snapshot). */
    @Test
    void testUpdateGameSetupCompleted() {
        gameModel.addObserver(mockView);
        Map<String, Totem> totemByPlayer = Map.of("p1", Totem.WHITE, "me", Totem.BLUE);
        List<String> turnOrder = List.of("p1", "me");
        Map<String, Integer> initialFood = Map.of("p1", 2, "me", 3);
        
        List<OfferTileInfo> offerTiles = List.of(new OfferTileInfo('A', 1, 1, 1, "p1"));
        List<TurnOrderSlotInfo> turnOrderSlots = List.of(new TurnOrderSlotInfo("p1", 0, 0));
        BoardSnapshot snapshot = new BoardSnapshot(
                List.of("c1"), List.of("c2"), List.of("b1"), List.of("b2"),
                offerTiles, turnOrderSlots, 10
        );

        gameModel.updateGameSetupCompleted(totemByPlayer, turnOrder, initialFood, snapshot);

        assertEquals(Totem.WHITE, gameModel.getTotem("p1"));
        assertEquals(turnOrder, gameModel.getTurnOrder());
        assertEquals(initialFood, gameModel.getFoodByPlayer());
        assertEquals(0, gameModel.getPpByPlayer().get("p1"));
        assertEquals(List.of("c1"), gameModel.getUpperRow());
        assertEquals(List.of("b1"), gameModel.getUpperRowBuildings());
        assertEquals(10, gameModel.getDeckRemainingCount());
        assertEquals(1, gameModel.getRemainingUpper().get("p1"));
        
        verify(mockView).onGameSetupCompleted(turnOrder, initialFood, snapshot);

        // Test with null snapshot
        gameModel.updateGameSetupCompleted(totemByPlayer, turnOrder, initialFood, null);
        assertTrue(gameModel.getUpperRow().isEmpty());
    }

    /** Verifies phase updates store phase/current player and refresh pick limits on action resolution. */
    @Test
    void testUpdateCurrentPhase() {
        gameModel.addObserver(mockView);
        List<String> resolutionOrder = List.of("p1", "me");
        
        // Test non-ACTION_RESOLUTION phase
        gameModel.updateCurrentPhase(PhaseType.TOTEM_PLACEMENT, "p1", resolutionOrder);
        assertEquals(PhaseType.TOTEM_PLACEMENT, gameModel.getCurrentPhase());
        assertEquals("p1", gameModel.getCurrentPlayer());
        assertEquals(resolutionOrder, gameModel.getTurnOrder());
        verify(mockView).onPhaseChanged(PhaseType.TOTEM_PLACEMENT, "p1", resolutionOrder);

        // Test ACTION_RESOLUTION phase (updates limits)
        List<OfferTileInfo> offerTiles = List.of(new OfferTileInfo('A', 1, 2, 3, "p1"));
        gameModel.updateGameSetupCompleted(Map.of(), List.of(), Map.of(), 
                new BoardSnapshot(List.of(), List.of(), List.of(), List.of(), offerTiles, List.of(), 0));
        
        gameModel.updateCurrentPhase(PhaseType.ACTION_RESOLUTION, "me", null);
        assertEquals(2, gameModel.getRemainingUpper().get("p1"));
        assertEquals(3, gameModel.getRemainingLower().get("p1"));
    }

    /** Verifies a current-player update is stored and forwarded to the view. */
    @Test
    void testUpdateCurrentPlayer() {
        gameModel.addObserver(mockView);
        gameModel.updateCurrentPlayer("p1");
        assertEquals("p1", gameModel.getCurrentPlayer());
        verify(mockView).onCurrentPlayerChanged("p1");
    }

    /** Verifies a turn-order update is stored and forwarded to the view. */
    @Test
    void testUpdateTurnOrder() {
        gameModel.addObserver(mockView);
        List<String> turnOrder = List.of("me", "p1");
        gameModel.updateTurnOrder(turnOrder);
        assertEquals(turnOrder, gameModel.getTurnOrder());
        verify(mockView).onTurnOrderEstablished(turnOrder);
    }

    /** Verifies placing a totem updates its position, the offer tile occupant and clears the turn-order slot. */
    @Test
    void testUpdateTotemPlaced() {
        gameModel.addObserver(mockView);
        gameModel.updateGameSetupCompleted(Map.of(), List.of(), Map.of(), 
                new BoardSnapshot(List.of(), List.of(), List.of(), List.of(), 
                new ArrayList<>(List.of(new OfferTileInfo('A', 0, 1, 1, null))), 
                new ArrayList<>(List.of(new TurnOrderSlotInfo("me", 0, 0))), 0));

        gameModel.updateTotemPlaced("me", 'A');
        assertEquals('A', gameModel.getTotemPositions().get("me"));
        assertNull(gameModel.getTurnOrderSlots().get(0).occupantNickname());
        assertEquals("me", gameModel.getOfferTiles().get(0).occupantNickname());
        verify(mockView).onTotemPlaced("me", 'A');
        verify(mockView, atLeastOnce()).onOfferTilesUpdated(any());
    }

    /** Verifies returning a totem clears its position, restores the turn-order slot and frees the offer tile. */
    @Test
    void testUpdateTotemReturned() {
        gameModel.addObserver(mockView);
        gameModel.updateGameSetupCompleted(Map.of(), List.of(), Map.of(), 
                new BoardSnapshot(List.of(), List.of(), List.of(), List.of(), 
                new ArrayList<>(List.of(new OfferTileInfo('A', 0, 1, 1, "me"))), 
                new ArrayList<>(List.of(new TurnOrderSlotInfo(null, 1, 2))), 0));

        gameModel.updateTotemReturned("me", 0);
        assertNull(gameModel.getTotemPositions().get("me"));
        assertEquals(0, gameModel.getTurnOrderPositions().get("me"));
        assertEquals("me", gameModel.getTurnOrderSlots().get(0).occupantNickname());
        assertNull(gameModel.getOfferTiles().get(0).occupantNickname());
        verify(mockView).onTotemReturned("me", 0);
    }

    /** Verifies a board update refreshes the card rows and deck count and notifies the view. */
    @Test
    void testUpdateBoard() {
        gameModel.addObserver(mockView);
        List<String> upper = List.of("u1");
        List<String> lower = List.of("l1");
        gameModel.updateBoard(upper, lower, 5);
        assertEquals(upper, gameModel.getUpperRow());
        assertEquals(lower, gameModel.getLowerRow());
        assertEquals(5, gameModel.getDeckRemainingCount());
        verify(mockView).onBoardUpdated(upper, lower, 5);
    }

    /** Verifies an era update stores the new era and building rows and notifies the view. */
    @Test
    void testUpdateEra() {
        gameModel.addObserver(mockView);
        List<String> upper = List.of("ub1");
        List<String> lower = List.of("lb1");
        gameModel.updateEra(Era.II, upper, lower);
        assertEquals(Era.II, gameModel.getCurrentEra());
        assertEquals(upper, gameModel.getUpperRowBuildings());
        assertEquals(lower, gameModel.getLowerRowBuildings());
        verify(mockView).onEraChanged(Era.II, upper, lower);
    }

    /** Verifies both the initialization and subsequent updates of a player's pick limits. */
    @Test
    void testUpdatePlayerLimits() {
        gameModel.addObserver(mockView);
        gameModel.updatePlayerLimitsInitialized("p1", 1, 2);
        assertEquals(1, gameModel.getRemainingUpper().get("p1"));
        assertEquals(2, gameModel.getRemainingLower().get("p1"));
        verify(mockView).onPlayerLimitsInitialized("p1", 1, 2);

        gameModel.updatePlayerLimits("p1", 0, 1);
        assertEquals(0, gameModel.getRemainingUpper().get("p1"));
        assertEquals(1, gameModel.getRemainingLower().get("p1"));
        verify(mockView).onPlayerLimitsUpdated("p1", 0, 1);
    }

    /** Verifies food and prestige resource updates are stored per player and forwarded. */
    @Test
    void testUpdatePlayerResource() {
        gameModel.addObserver(mockView);
        gameModel.updatePlayerResource("p1", ResourceType.FOOD, 10);
        assertEquals(10, gameModel.getFoodByPlayer().get("p1"));
        verify(mockView).onPlayerResourceChanged("p1", ResourceType.FOOD, 10);

        gameModel.updatePlayerResource("p1", ResourceType.PRESTIGE_POINTS, 20);
        assertEquals(20, gameModel.getPpByPlayer().get("p1"));
        verify(mockView).onPlayerResourceChanged("p1", ResourceType.PRESTIGE_POINTS, 20);
    }

    /** Verifies taking characters and buildings from either row removes them and assigns them to the player. */
    @Test
    void testUpdateCardTaken() {
        gameModel.addObserver(mockView);
        gameModel.updateBoard(new ArrayList<>(List.of("c1")), new ArrayList<>(List.of("c2")), 10);
        gameModel.updateEra(Era.I, new ArrayList<>(List.of("b1")), new ArrayList<>(List.of("b2")));

        // Take character from upper
        gameModel.updateCardTaken("me", "c1", CardType.CHARACTER, RowPosition.UPPER);
        assertTrue(gameModel.getUpperRow().isEmpty());
        assertTrue(gameModel.getCharactersByPlayer().get("me").contains("c1"));

        // Take character from lower
        gameModel.updateCardTaken("me", "c2", CardType.CHARACTER, RowPosition.LOWER);
        assertTrue(gameModel.getLowerRow().isEmpty());
        assertTrue(gameModel.getCharactersByPlayer().get("me").contains("c2"));

        // Take building from upper
        gameModel.updateCardTaken("me", "b1", CardType.BUILDING, RowPosition.UPPER);
        assertTrue(gameModel.getUpperRowBuildings().isEmpty());
        assertTrue(gameModel.getBuildingsByPlayer().get("me").contains("b1"));

        // Take building from lower
        gameModel.updateCardTaken("me", "b2", CardType.BUILDING, RowPosition.LOWER);
        assertTrue(gameModel.getLowerRowBuildings().isEmpty());
        assertTrue(gameModel.getBuildingsByPlayer().get("me").contains("b2"));

        verify(mockView, times(4)).onCardTaken(anyString(), anyString(), any(), any());
    }

  /** Verifies auto-player timer-started and invoked updates are forwarded to the view. */
  @Test
    void testAutoPlayer() {
        gameModel.addObserver(mockView);

        long timeout = 30L;
        gameModel.updateAutoPlayerTimerStarted("p1", timeout);
        verify(mockView).onAutoPlayerTimerStarted("p1", timeout);

        gameModel.updateAutoPlayerInvoked("p1");
        verify(mockView).onAutoPlayerInvoked("p1");
    }

    /** Verifies a resolved event is stored as the last event and forwarded to the view. */
    @Test
    void testUpdateEventResolved() {
        gameModel.addObserver(mockView);
        gameModel.updateEventResolved("e1", "Event Name");
        assertEquals("Event Name", gameModel.getLastEventResolved());
        verify(mockView).onEventResolved("e1", "Event Name");
    }

    /** Verifies extra-turn start sets the player's pick limits and that start/end are forwarded. */
    @Test
    void testExtraTurn() {
        gameModel.addObserver(mockView);
        gameModel.updateExtraTurnStarted("me", 1, 1);
        assertEquals(1, gameModel.getRemainingUpper().get("me"));
        verify(mockView).onExtraTurnStarted("me", 1, 1);

        gameModel.updateExtraTurnEnded("me");
        verify(mockView).onExtraTurnEnded("me");
    }

    /** Verifies game end sets the ended flag and stores winners and final rankings. */
    @Test
    void testUpdateGameEnded() {
        gameModel.addObserver(mockView);
        List<String> winners = List.of("me");
        List<PlayerFinalScore> rankings = List.of(new PlayerFinalScore("me", 100, 20, 30, 40, 10));
        gameModel.updateGameEnded(winners, rankings);
        assertTrue(gameModel.isGameEnded());
        assertEquals(winners, gameModel.getWinners());
        assertEquals(rankings, gameModel.getFinalRankings());
        verify(mockView).onGameEnded(winners, rankings);
    }

    /** Verifies player disconnect/reconnect updates record the message and notify the view. */
    @Test
    void testPlayerDisconnectedReconnected() {
        gameModel.addObserver(mockView);
        gameModel.updatePlayerDisconnected("p1");
        assertTrue(gameModel.getLastErrorMessage().contains("p1"));
        verify(mockView).onPlayerDisconnected("p1");

        gameModel.updatePlayerReconnected("p1");
        verify(mockView).onPlayerReconnected("p1");
    }

    /** Verifies game abort and recovery-failure both mark the game ended and notify the view. */
    @Test
    void testGameAbortedAndRecoveryFailed() {
        gameModel.addObserver(mockView);
        gameModel.updateGameAborted("me");
        assertTrue(gameModel.isGameEnded());
        verify(mockView).onGameAborted("me");

        gameModel.updateGameRecoveryFailed();
        assertTrue(gameModel.isGameEnded());
        verify(mockView).onGameRecoveryFailed();
    }

    /** Verifies an error update stores the message and forwards it to the view. */
    @Test
    void testUpdateError() {
        gameModel.addObserver(mockView);
        gameModel.updateError("fatal error");
        assertEquals("fatal error", gameModel.getLastErrorMessage());
        verify(mockView).onError("fatal error");
    }

    /** Verifies basic accessors, the in-game flag toggling on game end, and that collections are non-null. */
    @Test
    void testGettersAndSetters() {
        assertEquals(myNickname, gameModel.getMyNickname());
        gameModel.setGameId("g1");
        assertEquals("g1", gameModel.getGameId());
        assertTrue(gameModel.isInGame());
        
        gameModel.updateGameEnded(List.of(), List.of());
        assertFalse(gameModel.isInGame());
        
        // Test remaining getters
        assertNotNull(gameModel.getTotemPositions());
        assertNotNull(gameModel.getTurnOrderPositions());
        assertNotNull(gameModel.getCharactersByPlayer());
        assertNotNull(gameModel.getBuildingsByPlayer());
        assertNotNull(gameModel.getOfferTiles());
        assertNotNull(gameModel.getTurnOrderSlots());
    }
}
