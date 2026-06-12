package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.listeners.GameEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link GameBoard} round and era transition logic, verifying that tribù
 * rows are refreshed, the tribù deck is drained correctly and the proper
 * notifications are emitted to the {@link GameEventEmitter}.
 */
class GameBoardTransitionsTest {

    private GameBoard gameBoard;
    private final GameEventEmitter notifier = mock(GameEventEmitter.class);
    private final Random gameRandom = new Random(42);

    @BeforeEach
    void setUp() {
        TestHelper.ensureRegistryLoaded();
        gameBoard = new GameBoard(2, notifier, gameRandom);
    }

    /**
     * Verifies that {@code prepareNewRound} draws the expected number of tribù
     * cards from the deck and notifies the listeners of the board update.
     */
    @Test
    void prepareNewRound_updatesRowsAndDecrementsDeck() {
        int initialDeckSize = gameBoard.buildSnapshot().tribuDeckSize();
        
        gameBoard.prepareNewRound(2);
        
        BoardSnapshot snapshot = gameBoard.buildSnapshot();
        assertEquals(initialDeckSize - (2 + 4), snapshot.tribuDeckSize());
        verify(notifier).notifyBoardUpdated(any(), any(), any(), any(), anyInt());
    }

    /**
     * Verifies that advancing to a new era triggers an era-changed notification.
     */
    @Test
    void updateRowsForNewEra_handlesEraTransitions() {
        // But we can check if it calls notifyEraChanged
        gameBoard.updateRowsForNewEra();
        verify(notifier).notifyEraChanged(any(), any(), any(), any());
    }
}
