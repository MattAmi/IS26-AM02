package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.error.ErrorEvent;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.lobby.GameStartedEvent;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.server.controller.GameController;
import it.polimi.ingsw.am02.server.controller.persistence.NoOpCommandLogger;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.tile.OfferTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test covering one full round of MESOS:
 * SetUp → TotemPlacement → ActionResolution (all players) → EndPlayerTurn → EndRound → NewRound.
 *
 * The turn order is random (Game uses shuffle), so after startFSM() we read
 * getTurnOrder() and getCurrentPlayerNickname() to drive the test dynamically.
 *
 * Two players: Matteo (WHITE) and Husnain (RED).
 */
class FullRoundIntegrationTest {

    private static final String P1 = "Matteo";
    private static final String P2 = "Husnain";
    private static final long seed = 42L;

    private Game game;
    private EventCollector collector;

    /** Collects all events broadcast by GameController. */
    static class EventCollector implements VirtualView {
        final List<Event> events = new ArrayList<>();

        @Override public void notifyUsernameResult(String username, boolean isValid, String reason) {}
        @Override public void notifyGameStarted(String gameId) { events.add(new GameStartedEvent(gameId)); }
        @Override public void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {}
        @Override public void notifyCurrentLobbyUpdated(LobbyInfo lobby) {}
        @Override public void notifyLobbyDissolved(String lobbyID) {}

        @Override
        public void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer, List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
            events.add(new GameSetupCompletedEvent(totemByPlayer, turnOrder, initialFood, boardSnapshot));
        }

        @Override
        public void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
            events.add(new PhaseChangedEvent(phase, currentPlayer, resolutionOrder));
        }

        @Override public void notifyCurrentPlayerChanged(String nextPlayer) { events.add(new CurrentPlayerChangedEvent(nextPlayer)); }
        @Override public void notifyTurnOrderEstablished(List<String> turnOrder) { events.add(new TurnOrderEstablishedEvent(turnOrder)); }

        @Override
        public void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {
            events.add(new BoardUpdatedEvent(newUpperRow, newLowerRow, discardedCards, movedToLowerRow, deckRemainingCount));
        }

        @Override
        public void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {
            events.add(new EraChangedEvent(newEra, newUpperRowBuildings, newLowerRowBuildings, discardedBuildings));
        }

        @Override public void notifyTotemPlaced(String nickname, char tileID) { events.add(new TotemPlacedEvent(nickname, tileID)); }
        @Override public void notifyTotemReturned(String nickname, int turnOrderPosition) { events.add(new TotemReturnedEvent(nickname, turnOrderPosition)); }
        @Override public void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) { events.add(new CardTakenEvent(nickname, cardID, cardType, sourceRow)); }
        @Override public void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) { events.add(new PlayerLimitsInitializedEvent(nickname, remainingUpper, remainingLower)); }
        @Override public void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) { events.add(new PlayerLimitsUpdatedEvent(nickname, remainingUpper, remainingLower)); }
        @Override public void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) { events.add(new PlayerResourceChangedEvent(nickname, resource, newValue, delta)); }
        @Override public void notifyEventResolved(String eventID, String eventName) { events.add(new EventResolvedEvent(eventID, eventName)); }
        @Override public void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) { events.add(new ExtraTurnStartedEvent(nickname, remainingUpper, remainingLower)); }
        @Override public void notifyExtraTurnEnded(String nickname) { events.add(new ExtraTurnEndedEvent(nickname)); }
        @Override public void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) { events.add(new GameEndedEvent(winners, finalRankings)); }
        @Override public void notifyError(String message) { events.add(new ErrorEvent(message)); }
        @Override public void notifyPlayerDisconnected(String nickname) { events.add(new PlayerDisconnectedEvent(nickname)); }
        @Override public void notifyPlayerReconnected(String nickname) { events.add(new PlayerReconnectedEvent(nickname)); }
        @Override public void notifyAutoPlayerTimerStarted(String nickname, long seconds) { events.add(new AutoPlayerTimerStartedEvent(nickname, seconds)); }
        @Override public void notifyAutoPlayerInvoked(String nickname) { events.add(new AutoPlayerInvokedEvent(nickname)); }
        @Override public void notifyGameAborted(String winner) { events.add(new GameAbortedEvent(winner)); }
        @Override public void notifyGameRecoveryFailed() { events.add(new GameRecoveryFailedEvent()); }
        @Override public void notifyGlobalTimerStarted(long seconds) { events.add(new GlobalTimerStartedEvent(seconds)); }
        @Override public void notifyGlobalTimerCancelled() { events.add(new GlobalTimerCancelledEvent()); }

        /** Returns the last event of the given type, or null. */
        @SuppressWarnings("unchecked")
        <T extends Event> T last(Class<T> type) {
            for (int i = events.size() - 1; i >= 0; i--) {
                if (type.isInstance(events.get(i))) return (T) events.get(i);
            }
            return null;
        }

        /** Returns all events of the given type. */
        @SuppressWarnings("unchecked")
        <T extends Event> List<T> all(Class<T> type) {
            return events.stream()
                    .filter(type::isInstance)
                    .map(e -> (T) e)
                    .toList();
        }

        void clear() { events.clear(); }
    }

    @BeforeEach
    void setUp() {
        TestHelper.ensureRegistryLoaded();

        Map<String, Totem> totems = new LinkedHashMap<>();
        totems.put(P1, Totem.WHITE);
        totems.put(P2, Totem.RED);

        game = new Game("test-game-01", List.of(P1, P2), totems, seed);

        collector = new EventCollector();
        Map<String, VirtualView> views = Map.of(P1, collector, P2, collector);

        // GameController registers itself as GameObserver on construction
        new GameController("test-game-01", game, views, new NoOpCommandLogger());

        // Triggers SetUpState → TotemPlacementState automatically
        game.startFSM();
    }

    /** Verifies startFSM broadcasts setup completion and enters the TOTEM_PLACEMENT phase. */
    @Test
    void fullRound_setupCompletedAndTotemPlacementStarted() {
        // After startFSM(): SetUpState fires GameSetupCompletedEvent,
        // then transitions to TotemPlacementState which fires PhaseChangedEvent(TOTEM_PLACEMENT).
        assertNotNull(collector.last(GameSetupCompletedEvent.class),
                "GameSetupCompletedEvent should have been broadcast after setup");

        PhaseChangedEvent phaseEvent = collector.last(PhaseChangedEvent.class);
        assertNotNull(phaseEvent);
        assertEquals(PhaseType.TOTEM_PLACEMENT, phaseEvent.phase());

        assertNotNull(game.getCurrentPlayerNickname(),
                "A current player should be set after setup");
        assertEquals(2, game.getTurnOrder().size());
    }

    /** Verifies that once both totems are placed the game enters ACTION_RESOLUTION and initializes pick limits. */
    @Test
    void fullRound_bothPlayersPlaceTotems_thenActionResolutionStarts() {
        List<String> order = game.getTurnOrder(); // random, but deterministic within this run

        // Read offer tiles from the snapshot to find valid tile IDs
        GameSetupCompletedEvent setupEvent = collector.last(GameSetupCompletedEvent.class);
        assertNotNull(setupEvent);
        List<Character> tileIDs = setupEvent.boardSnapshot().offerTiles().stream()
                .map(OfferTileInfo::tileID)
                .toList();

        // Each player picks a different tile
        char tile0 = tileIDs.get(0);
        char tile1 = tileIDs.get(1);

        game.moveTotem(order.get(0), tile0);
        game.moveTotem(order.get(1), tile1);

        // Both totems placed → should now be in ACTION_RESOLUTION
        PhaseChangedEvent phaseEvent = collector.last(PhaseChangedEvent.class);
        assertEquals(PhaseType.ACTION_RESOLUTION, phaseEvent.phase(),
                "Phase should be ACTION_RESOLUTION after all totems placed");

        // Limits should have been initialized for the first acting player
        assertFalse(collector.all(PlayerLimitsInitializedEvent.class).isEmpty(),
                "PlayerLimitsInitializedEvent should have been fired");
    }

    /** Verifies a full round: both players resolve actions and return their totems, advancing the game past ACTION_RESOLUTION. */
    @Test
    void fullRound_eachPlayerResolvesAndReturnsTotem_thenEndRoundReached() {
        List<String> placementOrder = game.getTurnOrder();

        GameSetupCompletedEvent setupEvent = collector.last(GameSetupCompletedEvent.class);
        List<Character> tileIDs = setupEvent.boardSnapshot().offerTiles().stream()
                .map(OfferTileInfo::tileID)
                .toList();

        char tile0 = tileIDs.get(0);
        char tile1 = tileIDs.get(1);

        // --- Totem placement ---
        game.moveTotem(placementOrder.get(0), tile0);
        game.moveTotem(placementOrder.get(1), tile1);

        // --- Action resolution: each player picks 0 cards then returns totem ---
        // Resolution order may differ from placement order; read it from the event.
        PhaseChangedEvent arEvent = collector.last(PhaseChangedEvent.class);
        List<String> resolutionOrder = arEvent.resolutionOrder();
        assertNotNull(resolutionOrder, "resolutionOrder must be set on ACTION_RESOLUTION PhaseChangedEvent");

        for (String player : resolutionOrder) {
            // resolve with empty list = take no cards (valid only if no mandatory pick obligation)
            // If the tile allows 0 picks from lower row and 0 from upper, this passes.
            // Otherwise use the first available card from the snapshot.
            tryResolveOrSkip(player, setupEvent);
            game.moveTotem(player, 'T'); // return totem to TurnOrderTile
        }

        // After all players returned → EndRoundState → should reach NewRound or EventResolution
        PhaseChangedEvent lastPhase = collector.last(PhaseChangedEvent.class);
        assertTrue(
                lastPhase.phase() == PhaseType.END_ROUND
                        || lastPhase.phase() == PhaseType.EVENT_RESOLUTION
                        || lastPhase.phase() == PhaseType.NEW_ROUND
                        || lastPhase.phase() == PhaseType.TOTEM_PLACEMENT, // next round started
                "After all players finish, game should advance past ACTION_RESOLUTION. Got: " + lastPhase.phase()
        );
    }

    /**
     * Attempts to resolve actions for a player.
     * Picks characters until the player is allowed to finish their turn.
     */
    private void tryResolveOrSkip(String player, GameSetupCompletedEvent setupEvent) {
        Player p = game.getPlayerByNickname(player);
        while (!game.getGameBoard().canPlayerFinish(p)) {
            OfferTile tile = game.getGameBoard().getOfferTrack().getTileByPlayer(p);
            String toPick = null;
            if (tile.getRemainingUpper() > 0) {
                for (String id : game.getGameBoard().getUpperRow()) {
                    if (GameRegistry.getInstance().isCharacter(id)) {
                        toPick = id;
                        break;
                    }
                }
            }
            if (toPick == null && tile.getRemainingLower() > 0) {
                for (String id : game.getGameBoard().getLowerRow()) {
                    if (GameRegistry.getInstance().isCharacter(id)) {
                        toPick = id;
                        break;
                    }
                }
            }

            if (toPick != null) {
                game.resolveActions(player, List.of(toPick));
            } else {
                break; // No characters left to pick
            }
        }
    }
}
