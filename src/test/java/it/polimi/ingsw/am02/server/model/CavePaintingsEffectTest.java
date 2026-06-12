package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.effect.CavePaintingsEffect;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link CavePaintingsEffect}, verifying that a player's tribù is
 * awarded a per-artist prestige bonus when the artist requirement is met and a
 * prestige malus otherwise.
 */
class CavePaintingsEffectTest {

    private Player p1, p2;
    private Tribu t1, t2;
    private CavePaintingsEffect effect;
    private static final int MIN_ARTISTS = 2;
    private static final int MALUS = 4;
    private static final int BONUS_PER_ARTIST = 3;

    @BeforeEach
    void setUp() {
        p1 = mock(Player.class);
        p2 = mock(Player.class);
        t1 = mock(Tribu.class);
        t2 = mock(Tribu.class);

        when(p1.getTribu()).thenReturn(t1);
        when(p2.getTribu()).thenReturn(t2);
        when(p1.getNickname()).thenReturn("P1");
        when(p2.getNickname()).thenReturn("P2");

        effect = new CavePaintingsEffect(MIN_ARTISTS, MALUS, BONUS_PER_ARTIST);
    }

    /** Verifies that meeting the artist requirement grants the per-artist bonus. */
    @Test
    void applyEffect_metRequirement_addsBonus() {
        when(t1.getCharacterCount(CharacterType.ARTIST)).thenReturn(3); // >= 2
        when(t1.getPrestigePoints()).thenReturn(10);

        EffectOutcome outcome = effect.applyEffect(List.of(p1));

        verify(t1).addPrestigePoints(9); // 3 * 3
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(9, outcome.resourceDeltas().get(0).delta());
    }

    /** Verifies that failing the artist requirement applies the prestige malus. */
    @Test
    void applyEffect_failedRequirement_addsMalus() {
        when(t1.getCharacterCount(CharacterType.ARTIST)).thenReturn(1); // < 2
        when(t1.getPrestigePoints()).thenReturn(10);

        EffectOutcome outcome = effect.applyEffect(List.of(p1));

        verify(t1).addPrestigePoints(-4);
        assertEquals(-4, outcome.resourceDeltas().get(0).delta());
    }
}
