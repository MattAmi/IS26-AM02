package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.effect.GathererEffect;
import it.polimi.ingsw.am02.server.model.effect.NoEffect;
import it.polimi.ingsw.am02.server.model.effect.ShamanEffect;
import it.polimi.ingsw.am02.server.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterEffectsTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Alice", Totem.RED);
    }

    @Test
    void testGathererEffect() {
        GathererEffect effect = new GathererEffect(2);
        EffectOutcome outcome = effect.applyEffect(player);

        assertEquals(2, player.getTribu().getFoodDiscount());
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(2, outcome.resourceDeltas().get(0).delta());
    }

    @Test
    void testShamanEffect() {
        ShamanEffect effect = new ShamanEffect(3);
        EffectOutcome outcome = effect.applyEffect(player);

        assertEquals(3, player.getTribu().getShamanStars());
        assertEquals(1, outcome.resourceDeltas().size());
        assertEquals(3, outcome.resourceDeltas().get(0).delta());
    }

    @Test
    void testNoEffect() {
        NoEffect effect = new NoEffect();
        EffectOutcome outcome = effect.applyEffect(player);
        assertTrue(outcome.isEmpty());
    }
}
