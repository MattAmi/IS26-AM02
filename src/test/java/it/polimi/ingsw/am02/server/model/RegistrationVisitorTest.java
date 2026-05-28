package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void testVisitPhaseObserver() {
        PhaseObserver observer = Mockito.mock(PhaseObserver.class);
        visitor.visitPhaseObserver(observer);
        verify(game).attachPhaseObserver(observer);
    }

    @Test
    void testVisitTribuObserver() {
        TribuObserver observer = Mockito.mock(TribuObserver.class);
        visitor.visitTribuObserver(observer);
        verify(tribu).attachTribuObserver(observer);
    }

    @Test
    void testVisitEventObserver() {
        EventObserver observer = Mockito.mock(EventObserver.class);
        visitor.visitEventObserver(observer);
        verify(gameBoard).attachEventObserver(observer);
    }
}
