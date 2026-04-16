package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Game;
import it.polimi.ingsw.am02.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class ExtraTurnEffectTest {

    private Player mockPlayer;
    private Game mockGame;
    private EffectVisitor mockVisitor;
    private ExtraTurnEffect effect;

    // Strict test constants to prevent "cheating"
    private final String TEST_NICKNAME = "TestPlayer123";
    private final int EXTRA_UPPER_PICKS = 2;
    private final int EXTRA_LOWER_PICKS = 1;

    @BeforeEach
    void setUp() {
        // 1. Create the mocks
        mockPlayer = Mockito.mock(Player.class);
        mockGame = Mockito.mock(Game.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // 2. Instantiate the effect with strict expected values
        effect = new ExtraTurnEffect(mockPlayer, mockGame, EXTRA_UPPER_PICKS, EXTRA_LOWER_PICKS);
    }

    @Test
    void testAcceptCallsVisitPhaseObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    @Test
    void testOnPhaseChange_WithEndRoundPhase_ShouldEnqueueExtraTurnWithCorrectData() {
        // Arrange: The mock player MUST return our specific test nickname
        when(mockPlayer.getNickname()).thenReturn(TEST_NICKNAME);

        // Act: Trigger the phase change
        effect.onPhaseChange(PhaseType.END_ROUND);

        // Assert: We DO NOT use any(), anyInt(), or anyString() here.
        // We strictly verify that the exact values passed to the constructor
        // and extracted from the player are correctly routed to the Game.
        verify(mockPlayer, times(1)).getNickname();
        verify(mockGame, times(1)).enqueueExtraTurn(TEST_NICKNAME, EXTRA_UPPER_PICKS, EXTRA_LOWER_PICKS);
    }

    @Test
    void testOnPhaseChange_WithOtherPhases_ShouldDoNothing() {
        // Act: Trigger a phase change that is NOT END_ROUND
        effect.onPhaseChange(PhaseType.ACTION_RESOLUTION);

        // Assert: The effect must remain completely silent
        verify(mockPlayer, never()).getNickname();

        // Ensure no extra turn is queued under any circumstance
        verify(mockGame, never()).enqueueExtraTurn(anyString(), anyInt(), anyInt());
    }
}