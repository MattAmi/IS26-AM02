package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.server.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link Game} lifecycle: FSM startup and observer notification,
 * rejection of moves before the game starts, the test-only building injection
 * hook and {@link GameObserver} registration/removal.
 */
class GameTest {

    private Game game;
    private GameObserver observer;

    @BeforeEach
    void setUp() {
        observer = Mockito.mock(GameObserver.class);
        Map<String, Totem> totems = Map.of(
                "Alice", Totem.RED,
                "Bob", Totem.BLUE
        );
        game = new Game("game1", List.of("Alice", "Bob"), totems, 12345L);
        game.addGameObserver(observer);
    }

    /** Verifies starting the FSM notifies observers and initializes turn order. */
    @Test
    void testStartFSM() {
        game.startFSM();
        Mockito.verify(observer, Mockito.atLeastOnce()).onGameSetupCompleted(
                Mockito.anyMap(), Mockito.anyList(), Mockito.anyMap(), Mockito.any()
        );
        assertNotNull(game.getCurrentPlayerNickname());
        assertNotNull(game.getTurnOrder());
    }

    /** Verifies a move attempted before the FSM is started fails. */
    @Test
    void testInvalidMoveBeforeStart() {
        assertThrows(NullPointerException.class, () -> game.moveTotem("Alice", 'A'));
        // Or if currentState is not set, we shouldn't be able to move.
    }

    /** Verifies the test-only hook injects a building effect into a player's tribù. */
    @Test
    void testInjectBuildingForTesting() {
        game.startFSM();
        game.injectBuildingForTesting("Alice", "B_001");
        Player alice = game.getPlayerByNickname("Alice");
        assertNotNull(alice.getTribu().getActiveBuildingEffects());
    }

    /** Verifies a removed observer no longer receives game notifications. */
    @Test
    void testRemoveObserver() {
        game.removeGameObserver(observer);
        game.startFSM();
        Mockito.verify(observer, Mockito.never()).onGameSetupCompleted(
                Mockito.anyMap(), Mockito.anyList(), Mockito.anyMap(), Mockito.any()
        );
    }
}
