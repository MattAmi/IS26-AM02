package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.effect.ShamanicRitualEffect;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link ShamanicRitualEffect}, verifying that the shaman-star
 * majority is rewarded and the minority penalised, that a shared majority grants
 * no exclusive bonus, and that immune tribù avoid the malus.
 */
class ShamanicRitualEffectTest {

    private Player p1, p2, p3;
    private Tribu t1, t2, t3;
    private ShamanicRitualEffect effect;
    private static final int BONUS = 10;
    private static final int MALUS = 5;

    @BeforeEach
    void setUp() {
        p1 = mock(Player.class);
        p2 = mock(Player.class);
        p3 = mock(Player.class);
        t1 = mock(Tribu.class);
        t2 = mock(Tribu.class);
        t3 = mock(Tribu.class);

        when(p1.getTribu()).thenReturn(t1);
        when(p2.getTribu()).thenReturn(t2);
        when(p3.getTribu()).thenReturn(t3);
        when(p1.getNickname()).thenReturn("P1");
        when(p2.getNickname()).thenReturn("P2");
        when(p3.getNickname()).thenReturn("P3");

        effect = new ShamanicRitualEffect(BONUS, MALUS);
    }

    /** Verifies the clear star majority gets the bonus and the minority the malus. */
    @Test
    void applyEffect_clearMajorityAndMinority() {
        when(t1.getShamanStars()).thenReturn(5); // Majority
        when(t2.getShamanStars()).thenReturn(3); // Middle
        when(t3.getShamanStars()).thenReturn(1); // Minority

        EffectOutcome outcome = effect.applyEffect(List.of(p1, p2, p3));

        verify(t1).addPrestigePoints(BONUS);
        verify(t1).setLastEventBonusReceived(BONUS);
        verify(t3).addPrestigePoints(-MALUS);
        verify(t2, never()).addPrestigePoints(anyInt());
        
        assertEquals(2, outcome.resourceDeltas().size());
    }

    /** Verifies a tied majority still earns the bonus but is not marked an exclusive winner. */
    @Test
    void applyEffect_sharedMajority_noExclusiveWinner() {
        when(t1.getShamanStars()).thenReturn(5); // Majority shared
        when(t2.getShamanStars()).thenReturn(5); // Majority shared
        when(t3.getShamanStars()).thenReturn(1); // Minority

        effect.applyEffect(List.of(p1, p2, p3));

        verify(t1).addPrestigePoints(BONUS);
        verify(t1).setLastEventBonusReceived(0); // Not exclusive
        verify(t2).addPrestigePoints(BONUS);
        verify(t2).setLastEventBonusReceived(0); // Not exclusive
    }

    /** Verifies an immune minority tribù receives no prestige malus. */
    @Test
    void applyEffect_minorityImmune_noMalus() {
        when(t1.getShamanStars()).thenReturn(5);
        when(t2.getShamanStars()).thenReturn(1); // Minority
        when(t2.isImmune()).thenReturn(true);

        effect.applyEffect(List.of(p1, p2));

        verify(t1).addPrestigePoints(BONUS);
        verify(t2, never()).addPrestigePoints(anyInt());
    }
}
