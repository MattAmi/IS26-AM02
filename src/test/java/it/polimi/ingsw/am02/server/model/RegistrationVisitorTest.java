package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.effect.RegistrationVisitor;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RegistrationVisitor}, verifying that each observer type is
 * attached to the correct subject: phase observers to the {@link Game}, tribù
 * observers to the {@link Tribu} and event observers to the {@link GameBoard}.
 */
class RegistrationVisitorTest {

    private Game game;
    private Tribu tribu;
    private GameBoard gameBoard;
    private RegistrationVisitor visitor;

    @BeforeEach
    void setUp() {
        game = Mockito.mock(Game.class);
        tribu = Mockito.mock(Tribu.class);
        gameBoard = Mockito.mock(GameBoard.class);
        when(game.getGameBoard()).thenReturn(gameBoard);
        visitor = new RegistrationVisitor(game, tribu);
    }

    /** Verifies a phase observer is registered on the game. */
    @Test
    void testVisitPhaseObserver() {
        PhaseObserver observer = Mockito.mock(PhaseObserver.class);
        visitor.visitPhaseObserver(observer);
        verify(game).attachPhaseObserver(observer);
    }

    /** Verifies a tribù observer is registered on the tribù. */
    @Test
    void testVisitTribuObserver() {
        TribuObserver observer = Mockito.mock(TribuObserver.class);
        visitor.visitTribuObserver(observer);
        verify(tribu).attachTribuObserver(observer);
    }

    /** Verifies an event observer is registered on the game board. */
    @Test
    void testVisitEventObserver() {
        EventObserver observer = Mockito.mock(EventObserver.class);
        visitor.visitEventObserver(observer);
        verify(gameBoard).attachEventObserver(observer);
    }
}
