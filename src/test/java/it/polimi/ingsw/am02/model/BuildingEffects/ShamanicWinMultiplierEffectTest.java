package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.EventCard;
import it.polimi.ingsw.am02.model.Enumerations.EventType;
import it.polimi.ingsw.am02.model.Game;
import it.polimi.ingsw.am02.model.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class ShamanicWinMultiplierEffectTest {

    private Tribu mockTribu;
    private Game mockGame;
    private EffectVisitor mockVisitor;
    private EventCard mockEventCard;
    private ShamanicWinMultiplierEffect effect;

    // Strict test constants (No Magic Numbers)
    private final int MULTIPLIER = 3;
    private final int BASE_MAJORITY_BONUS = 5;
    private final EventType TRIGGERING_EVENT = EventType.SHAMANIC_RITUAL;
    private final EventType NON_TRIGGERING_EVENT = getAlternativeEvent();

    @BeforeEach
    void setUp() {
        mockTribu = Mockito.mock(Tribu.class);
        mockGame = Mockito.mock(Game.class);
        mockVisitor = Mockito.mock(EffectVisitor.class);
        mockEventCard = Mockito.mock(EventCard.class);

        effect = new ShamanicWinMultiplierEffect(mockTribu, mockGame, MULTIPLIER);

        // Default setup for the EventCard
        when(mockEventCard.getMajorityBonus()).thenReturn(BASE_MAJORITY_BONUS);
    }

    @Test
    void testAcceptCallsVisitEventObserver() {
        effect.accept(mockVisitor);
        verify(mockVisitor, times(1)).visitEventObserver(effect);
    }

    @Test
    void testEventStart_ShouldDoNothing() {
        // Act: The source code specifically leaves this blank
        effect.EventStart(TRIGGERING_EVENT);

        // Assert: Ensure absolutely no interactions happen
        verifyNoInteractions(mockTribu);
        verifyNoInteractions(mockGame);
    }

    @Test
    void testEventResolution_WithDifferentEvent_ShouldDoNothing() {
        // Act
        effect.EventResolution(NON_TRIGGERING_EVENT, mockEventCard);

        // Assert: Ensure Game and Tribu aren't queried for stats during a drought or famine
        verify(mockGame, never()).calculateMaxShamanStars();
        verify(mockTribu, never()).getShamanStars();
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }

    // --- LOGIC AND MATH EVALUATION TESTS ---

    @Test
    void testEventResolution_WhenPlayerWins_ShouldApplyMultiplier() {
        // Arrange: Player has 4 stars, max on the board is 3. (Clear victory)
        when(mockTribu.getShamanStars()).thenReturn(4);
        when(mockGame.calculateMaxShamanStars()).thenReturn(3);

        // Expected math: 5 * (3 - 1) = 10
        int expectedExtraBonus = BASE_MAJORITY_BONUS * (MULTIPLIER - 1);

        // Act
        effect.EventResolution(TRIGGERING_EVENT, mockEventCard);

        // Assert: The extra bonus must be exactly calculated
        verify(mockTribu, times(1)).addPrestigePoints(expectedExtraBonus);
    }

    @Test
    void testEventResolution_WhenPlayerTiesForFirst_ShouldApplyMultiplier() {
        // Arrange: Player has 4 stars, max on the board is 4. (Tie for first place)
        when(mockTribu.getShamanStars()).thenReturn(4);
        when(mockGame.calculateMaxShamanStars()).thenReturn(4);

        int expectedExtraBonus = BASE_MAJORITY_BONUS * (MULTIPLIER - 1);

        // Act
        effect.EventResolution(TRIGGERING_EVENT, mockEventCard);

        // Assert: The ">= maxStars" logic should recognize the tie and award the points
        verify(mockTribu, times(1)).addPrestigePoints(expectedExtraBonus);
    }

    @Test
    void testEventResolution_WhenPlayerLoses_ShouldNotApplyBonus() {
        // Arrange: Player has 2 stars, max on the board is 4.
        when(mockTribu.getShamanStars()).thenReturn(2);
        when(mockGame.calculateMaxShamanStars()).thenReturn(4);

        // Act
        effect.EventResolution(TRIGGERING_EVENT, mockEventCard);

        // Assert: The condition fails, so no points are added
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }

    @Test
    void testEventResolution_WhenTieAtZeroStars_ShouldNotApplyBonus() {
        // Arrange: The crucial edge case. Everyone has 0 stars.
        when(mockTribu.getShamanStars()).thenReturn(0);
        when(mockGame.calculateMaxShamanStars()).thenReturn(0);

        // Act
        effect.EventResolution(TRIGGERING_EVENT, mockEventCard);

        // Assert: The "maxStars > 0" safety check must prevent points from being awarded
        verify(mockTribu, never()).addPrestigePoints(anyInt());
    }

    @Test
    void testEventResolution_WithMultiplierOfOne_ShouldAddZeroExtraPoints() {
        // Arrange: Multiplier is 1, meaning no EXTRA bonus (base bonus handles itself elsewhere)
        int multiplierOfOne = 1;
        ShamanicWinMultiplierEffect weakEffect = new ShamanicWinMultiplierEffect(mockTribu, mockGame, multiplierOfOne);

        when(mockTribu.getShamanStars()).thenReturn(5);
        when(mockGame.calculateMaxShamanStars()).thenReturn(5);

        // Expected math: 5 * (1 - 1) = 0
        int expectedExtraBonus = 0;

        // Act
        weakEffect.EventResolution(TRIGGERING_EVENT, mockEventCard);

        // Assert: Strictly verify that exactly 0 is added, proving the formula evaluates correctly
        verify(mockTribu, times(1)).addPrestigePoints(expectedExtraBonus);
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