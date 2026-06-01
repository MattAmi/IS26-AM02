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

    @Test
    void testStartFSM() {
        game.startFSM();
        Mockito.verify(observer, Mockito.atLeastOnce()).onGameSetupCompleted(
                Mockito.anyMap(), Mockito.anyList(), Mockito.anyMap(), Mockito.any()
        );
        assertNotNull(game.getCurrentPlayerNickname());
        assertNotNull(game.getTurnOrder());
    }

    @Test
    void testInvalidMoveBeforeStart() {
        assertThrows(NullPointerException.class, () -> game.moveTotem("Alice", 'A'));
        // Or if currentState is not set, we shouldn't be able to move.
    }

    @Test
    void testInjectBuildingForTesting() {
        game.startFSM();
        game.injectBuildingForTesting("Alice", "B_001");
        Player alice = game.getPlayerByNickname("Alice");
        assertNotNull(alice.getTribu().getActiveBuildingEffects());
    }

    @Test
    void testRemoveObserver() {
        game.removeGameObserver(observer);
        game.startFSM();
        Mockito.verify(observer, Mockito.never()).onGameSetupCompleted(
                Mockito.anyMap(), Mockito.anyList(), Mockito.anyMap(), Mockito.any()
        );
    }
}
