package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Game;
import it.polimi.ingsw.am02.model.Player;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class TurnOrderFoodBonusEffectTest {

    private Player mockPlayer;
    private Game mockGame;
    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private TurnOrderFoodBonusEffect effect;

    // We use a constant for the triggering phase
    private final PhaseType TRIGGERING_PHASE = PhaseType.END_PLAYER_TURN;

    @BeforeEach
    void setUp() {
        mockPlayer = Mockito.mock(Player.class);
        mockGame = Mockito.mock(Game.class);
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        effect = new TurnOrderFoodBonusEffect(mockPlayer, mockGame);

        // Crucial setup: The player must return our specific mockTribu
        // This is how we prove the effect isn't passing a random or null object.
        when(mockPlayer.getTribu()).thenReturn(mockTribu);
    }

    @Test
    void testAcceptCallsVisitPhaseObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    @Test
    void testOnPhaseChange_WithEndPlayerTurn_ShouldTriggerGameMethodWithCorrectTribu() {
        // Act: Fire the phase change
        effect.onPhaseChange(TRIGGERING_PHASE);

        // Assert 1: Prove the effect actually asked the player for their Tribu
        verify(mockPlayer, times(1)).getTribu();

        // Assert 2: Prove the effect passed EXACTLY that Tribu to the Game object.
        // If a teammate writes game.triggerTurnOrderExtraFood(new Tribu()) this will fail!
        verify(mockGame, times(1)).triggerTurnOrderExtraFood(mockTribu);
    }


    @Test
    void testOnPhaseChange_WithDifferentPhase_ShouldDoNothing() {
        // Act: Fire a phase that should NOT trigger the effect (e.g., GAME_SETUP)
        effect.onPhaseChange(PhaseType.ACTION_RESOLUTION);

        // Assert: The effect must be completely silent.
        // It shouldn't ask the player for anything, and it shouldn't talk to the game.
        verify(mockPlayer, never()).getTribu();
        verify(mockGame, never()).triggerTurnOrderExtraFood(any());
    }
}