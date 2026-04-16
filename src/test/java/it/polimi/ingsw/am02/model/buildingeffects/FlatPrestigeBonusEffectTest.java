package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class FlatPrestigeBonusEffectTest {

    private Tribu mockTribu;
    private EffectVisitor mockVisitor;
    private FlatPrestigeBonusEffect effect;

    // Strict test constants (No Magic Numbers)
    private final int FLAT_BONUS_PP = 5;

    @BeforeEach
    void setUp() {
        // 1. Create the mocks
        mockTribu = Mockito.mock(Tribu.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        // 2. Instantiate the effect
        effect = new FlatPrestigeBonusEffect(mockTribu, FLAT_BONUS_PP);
    }

    @Test
    void testAcceptCallsVisitPhaseObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitPhaseObserver(effect);
    }

    @Test
    void testOnPhaseChange_WithEndGamePhase_ShouldAddExactBonus() {
        // Act: Trigger the phase change
        effect.onPhaseChange(PhaseType.END_GAME);

        // Assert: Verify that the exact flat bonus amount is given to the Tribu
        verify(mockTribu, times(1)).addPrestigePoints(FLAT_BONUS_PP);
    }

    @Test
    void testOnPhaseChange_WithOtherPhases_ShouldDoNothing() {
        // Act: Trigger a phase change that is NOT END_GAME
        effect.onPhaseChange(PhaseType.ACTION_RESOLUTION);

        // Assert: The effect must remain silent
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }
}