package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.exceptions.InvalidMoveException;
import it.polimi.ingsw.am02.server.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * White-box tests for the {@link Game} finite-state machine. Using reflection to
 * instantiate and drive each inner FSM state with a mocked {@link GameBoard},
 * verifies the entry actions and the moves each state accepts or rejects with
 * {@link InvalidMoveException}.
 */
public class GameCoverageTest {

    private Game game;
    private GameBoard mockGameBoard;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game("test-game", List.of("p1", "p2"), Map.of("p1", Totem.RED, "p2", Totem.PURPLE), 123L);
        mockGameBoard = mock(GameBoard.class);

        // FIX: Istruiamo il mock a restituire una lista finta di giocatori
        // così quando gli stati transitano a NewRoundState non crashano su .getFirst()
        Player p1 = mock(Player.class);
        when(p1.getNickname()).thenReturn("p1");
        Player p2 = mock(Player.class);
        when(p2.getNickname()).thenReturn("p2");
        when(mockGameBoard.getPlayersInPlacementOrder()).thenReturn(List.of(p1, p2));
        // -------------------------------------------------------------

        Field gameBoardField = Game.class.getDeclaredField("gameBoard");
        gameBoardField.setAccessible(true);
        gameBoardField.set(game, mockGameBoard);

        Field turnOrderField = Game.class.getDeclaredField("turnOrder");
        turnOrderField.setAccessible(true);
        turnOrderField.set(game, new java.util.ArrayList<>(List.of("p1", "p2")));

        Field currentPlayerNicknameField = Game.class.getDeclaredField("currentPlayerNickname");
        currentPlayerNicknameField.setAccessible(true);
        currentPlayerNicknameField.set(game, "p1");
    }

    private Object createInnerClassInstance(String className) throws Exception {
        Class<?> clazz = Class.forName("it.polimi.ingsw.am02.server.model.Game$" + className);
        Constructor<?> constructor = clazz.getDeclaredConstructor(Game.class);
        constructor.setAccessible(true);
        return constructor.newInstance(game);
    }

    private void invokeMethod(Object instance, String methodName, Class<?>[] argTypes, Object... args) throws Exception {
        Method method = null;
        Class<?> clazz = instance.getClass();

        // Risale la gerarchia delle classi (es. dalla classe anonima a BaseState)
        while (clazz != null) {
            try {
                method = clazz.getDeclaredMethod(methodName, argTypes);
                break; // Metodo trovato!
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (method == null) {
            throw new NoSuchMethodException("Metodo '" + methodName + "' non trovato in " + instance.getClass().getName());
        }

        method.setAccessible(true);
        try {
            method.invoke(instance, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Estrae e rilancia l'eccezione VERA generata dal gioco (es. InvalidMoveException)
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    private void invokeMethod(Object instance, String methodName) throws Exception {
        invokeMethod(instance, methodName, new Class<?>[]{});
    }

    private void invokeMoveTotem(Object instance, String nickname, char tileID) throws Exception {
        invokeMethod(instance, "moveTotem", new Class<?>[]{String.class, char.class}, nickname, tileID);
    }

    private void invokeResolveActions(Object instance, String nickname, List<String> selectedIDs) throws Exception {
        invokeMethod(instance, "resolveActions", new Class<?>[]{String.class, List.class}, nickname, selectedIDs);
    }

    /** Verifies EventResolutionState runs its entry actions and rejects player moves. */
    @Test
    void testEventResolutionState() throws Exception {
        Object state = createInnerClassInstance("EventResolutionState");
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));
        assertThrows(InvalidMoveException.class, () -> invokeMoveTotem(state, "p1", 'A'));
        assertThrows(InvalidMoveException.class, () -> invokeResolveActions(state, "p1", Collections.emptyList()));
    }

    /** Verifies FinalEventsResolutionState runs its entry actions and rejects player moves. */
    @Test
    void testFinalEventsResolutionState() throws Exception {
        Object state = createInnerClassInstance("FinalEventsResolutionState");
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));
        assertThrows(InvalidMoveException.class, () -> invokeMoveTotem(state, "p1", 'A'));
        assertThrows(InvalidMoveException.class, () -> invokeResolveActions(state, "p1", Collections.emptyList()));
    }

    /** Verifies FinalScoringState runs its entry actions and rejects player moves. */
    @Test
    void testFinalScoringState() throws Exception {
        Object state = createInnerClassInstance("FinalScoringState");
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));
        assertThrows(InvalidMoveException.class, () -> invokeMoveTotem(state, "p1", 'A'));
        assertThrows(InvalidMoveException.class, () -> invokeResolveActions(state, "p1", Collections.emptyList()));
    }

    /** Verifies NewEraState runs its entry actions and rejects player moves. */
    @Test
    void testNewEraState() throws Exception {
        Object state = createInnerClassInstance("NewEraState");
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));
        assertThrows(InvalidMoveException.class, () -> invokeMoveTotem(state, "p1", 'A'));
        assertThrows(InvalidMoveException.class, () -> invokeResolveActions(state, "p1", Collections.emptyList()));
    }

    /**
     * Verifies ActionResolutionState handles action resolution, extra-turn mode and
     * the various end-of-turn ('T') transitions depending on round events and finish state.
     */
    @Test
    void testActionResolutionState() throws Exception {
        Object state = createInnerClassInstance("ActionResolutionState");
        
        // resolveActions
        assertDoesNotThrow(() -> invokeResolveActions(state, "p1", Collections.emptyList()));
        
        // Extra turn mode resolveActions
        Field extraTurnField = Game.class.getDeclaredField("isExtraTurnMode");
        extraTurnField.setAccessible(true);
        extraTurnField.set(game, true);
        assertDoesNotThrow(() -> invokeResolveActions(state, "p1", Collections.emptyList()));
        
        // moveTotem - not T
        assertThrows(InvalidMoveException.class, () -> invokeMoveTotem(state, "p1", 'A'));

        // moveTotem - T, isExtraTurnMode = true
        when(mockGameBoard.hasRoundEvents()).thenReturn(true);
        assertDoesNotThrow(() -> invokeMoveTotem(state, "p1", 'T'));

        // moveTotem - T, isExtraTurnMode = true, no round events
        extraTurnField.set(game, true);
        when(mockGameBoard.hasRoundEvents()).thenReturn(false);
        assertDoesNotThrow(() -> invokeMoveTotem(state, "p1", 'T'));

        // moveTotem - T, isExtraTurnMode = false
        extraTurnField.set(game, false);
        when(mockGameBoard.canPlayerFinish(any())).thenReturn(true);
        assertDoesNotThrow(() -> invokeMoveTotem(state, "p1", 'T'));

        // moveTotem - T, isExtraTurnMode = false, cannot finish
        when(mockGameBoard.canPlayerFinish(any())).thenReturn(false);
        assertThrows(Exception.class, () -> invokeMoveTotem(state, "p1", 'T'));
    }

    /** Verifies NewRoundState entry actions across era change, final events and round limits. */
    @Test
    void testNewRoundState() throws Exception {
        Object state = createInnerClassInstance("NewRoundState");

        when(mockGameBoard.hasEraChanged()).thenReturn(true);
        Player p1 = mock(Player.class);
        when(p1.getNickname()).thenReturn("p1");
        when(mockGameBoard.getPlayersInPlacementOrder()).thenReturn(List.of(p1));
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));

        when(mockGameBoard.hasEraChanged()).thenReturn(false);
        Field completedRounds = Game.class.getDeclaredField("completedRounds");
        completedRounds.setAccessible(true);
        completedRounds.set(game, 11);
        when(mockGameBoard.hasFinalEvents()).thenReturn(true);
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));

        when(mockGameBoard.hasFinalEvents()).thenReturn(false);
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));

        completedRounds.set(game, 5);
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));
    }

    /** Verifies EndRoundState entry actions for extra-turn, round-event and plain-round paths. */
    @Test
    void testEndRoundState() throws Exception {
        Object state = createInnerClassInstance("EndRoundState");

        Field extraTurnField = Game.class.getDeclaredField("extraTurnPlayerNickname");
        extraTurnField.setAccessible(true);
        extraTurnField.set(game, "p1");
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));

        extraTurnField.set(game, null);
        when(mockGameBoard.hasRoundEvents()).thenReturn(true);
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));

        when(mockGameBoard.hasRoundEvents()).thenReturn(false);
        assertDoesNotThrow(() -> invokeMethod(state, "onEntryActions"));
    }

}
