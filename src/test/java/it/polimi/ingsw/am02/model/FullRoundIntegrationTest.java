package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.server.controller.GameController;
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

    private Game game;
    private EventCollector collector;

    /** Collects all events broadcast by GameController. */
    static class EventCollector implements VirtualView {
        final List<Event> events = new ArrayList<>();

        @Override
        public void notify(Event event) {
            events.add(event);
        }

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
        new GameController(game, views);

        // Triggers SetUpState → TotemPlacementState automatically
        game.startFSM();
    }

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

    @Test
    void fullRound_bothPlayersPlaceTotems_thenActionResolutionStarts() {
        List<String> order = game.getTurnOrder(); // random, but deterministic within this run

        // Read offer tiles from the snapshot to find valid tile IDs
        GameSetupCompletedEvent setupEvent = collector.last(GameSetupCompletedEvent.class);
        assertNotNull(setupEvent);
        List<Character> tileIDs = setupEvent.boardSnapshot().offerTiles().stream()
                .map(t -> t.tileID())
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

    @Test
    void fullRound_eachPlayerResolvesAndReturnsTotem_thenEndRoundReached() {
        List<String> placementOrder = game.getTurnOrder();

        GameSetupCompletedEvent setupEvent = collector.last(GameSetupCompletedEvent.class);
        List<Character> tileIDs = setupEvent.boardSnapshot().offerTiles().stream()
                .map(t -> t.tileID())
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
     * Passes empty list first; if the model throws (mandatory pick), falls back
     * to picking the first available upper-row card.
     */
    private void tryResolveOrSkip(String player, GameSetupCompletedEvent setupEvent) {
        try {
            game.resolveActions(player, List.of());
        } catch (RuntimeException e) {
            // Mandatory pick required — take the first upper-row card available
            List<String> upper = setupEvent.boardSnapshot().upperRowCards();
            if (!upper.isEmpty()) {
                game.resolveActions(player, List.of(upper.get(0)));
            }
        }
    }
}