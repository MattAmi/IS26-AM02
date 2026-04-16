package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.buildingeffects.ShamanicWinMultiplierEffect;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class ShamanicWinMultiplierEffectTest {

    private Tribu mockTribu;
    private Game mockGame; // Note: Currently dead code in the source class, mocked to satisfy the constructor
    private EffectVisitor mockVisitor;
    private ShamanicWinMultiplierEffect effect;

    // Strict test constants (No Magic Numbers)
    private final int MULTIPLIER = 3;
    private final int BASE_BONUS_RECEIVED = 5;
    private final EventType TRIGGERING_EVENT = EventType.SHAMANIC_RITUAL;
    private final EventType NON_TRIGGERING_EVENT = getAlternativeEvent();

    @BeforeEach
    void setUp() {
        mockTribu = Mockito.mock(Tribu.class);
        mockGame = Mockito.mock(Game.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);

        effect = new ShamanicWinMultiplierEffect(mockTribu, mockGame, MULTIPLIER);
    }

    @Test
    void testAcceptCallsVisitEventObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitEventObserver(effect);
    }

    // --- LOGIC AND MATH EVALUATION TESTS ---

    @Test
    void testEventPostResolution_WithShamanicRitual_WhenBonusReceived_ShouldApplyMultiplier() {
        // Arrange: The Tribu confirms it received a positive bonus from the event
        when(mockTribu.getLastEventBonusReceived()).thenReturn(BASE_BONUS_RECEIVED);

        // Expected math: 5 * (3 - 1) = 10 extra points
        int expectedExtraBonus = BASE_BONUS_RECEIVED * (MULTIPLIER - 1);

        // Act
        effect.EventPostResolution(TRIGGERING_EVENT);

        // Assert: Verify the exact extra bonus is calculated and added
        verify(mockTribu, times(1)).addPrestigePoints(expectedExtraBonus);
    }

    @Test
    void testEventPostResolution_WithShamanicRitual_WhenNoBonusReceived_ShouldNotApplyMultiplier() {
        // Arrange: The Tribu lost the tie or the event, receiving 0 bonus points
        int zeroBonus = 0;
        when(mockTribu.getLastEventBonusReceived()).thenReturn(zeroBonus);

        // Act
        effect.EventPostResolution(TRIGGERING_EVENT);

        // Assert: The "> 0" condition must block any points from being added
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }

    @Test
    void testEventPostResolution_WithMultiplierOfOne_ShouldAddZeroExtraPoints() {
        // Arrange: Multiplier is 1, meaning no EXTRA bonus should be given
        int multiplierOfOne = 1;
        ShamanicWinMultiplierEffect weakEffect = new ShamanicWinMultiplierEffect(mockTribu, mockGame, multiplierOfOne);

        when(mockTribu.getLastEventBonusReceived()).thenReturn(BASE_BONUS_RECEIVED);

        // Expected math: 5 * (1 - 1) = 0
        int expectedExtraBonus = 0;

        // Act
        weakEffect.EventPostResolution(TRIGGERING_EVENT);

        // Assert: Strictly verify that exactly 0 is added, proving the formula evaluates correctly
        verify(mockTribu, times(1)).addPrestigePoints(expectedExtraBonus);
    }

    @Test
    void testEventPostResolution_WithDifferentEvent_ShouldDoNothing() {
        // Act: Trigger an event that is NOT SHAMANIC_RITUAL
        effect.EventPostResolution(NON_TRIGGERING_EVENT);

        // Assert: Ensure the effect is completely silent.
        // It should not even ask the Tribu for its last bonus.
        verify(mockTribu, never()).getLastEventBonusReceived();
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }

    // --- HELPER METHOD ---

    private EventType getAlternativeEvent() {
        for (EventType type : EventType.values()) {
            if (type != EventType.SHAMANIC_RITUAL) {
                return type;
            }
        }
        throw new IllegalStateException("EventType enum needs at least one other event besides SHAMANIC_RITUAL!");
    }
}