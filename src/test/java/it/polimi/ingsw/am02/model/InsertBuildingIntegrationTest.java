package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.buildingeffects.FlatPrestigeBonusEffect;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Enumerations.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Level 3 integration tests: Tribu.insertBuilding with a real Game.
 * Verifies the full pipeline: GameRegistry → BuildingFactory → BuildingEffect
 * → RegistrationVisitor → storage in Tribu → effect activation.
 *
 * Only Game.getPlayerByNickname needs to be package-private.
 * Phase notifications are tested by calling onPhaseChange directly
 * on the effect (same approach as unit tests, but with real objects).
 */
class InsertBuildingIntegrationTest {

    private static GameRegistry registry;
    private static final String BUILDING_ID = "B_021"; // FLAT_ENDGAME_PP

    private Game game;
    private Player player1;
    private Player player2;
    private Tribu tribu1;
    private Tribu tribu2;

    @BeforeEach
    void setUp() {
        TestHelper.ensureRegistryLoaded();
        registry = GameRegistry.getInstance();
        List<String> nicknames = List.of("Matteo", "Raed");
        Map<String, Totem> mappa = Map.of("Matteo", Totem.WHITE, "Raed", Totem.BLUE);

        game = new Game("000", nicknames, mappa);

        // Only method that needs to be package-private in Game
        player1 = game.getPlayerByNickname("Matteo");
        player2 = game.getPlayerByNickname("Raed");
        tribu1 = player1.getTribu();
        tribu2 = player2.getTribu();
    }

    // Test 1: insertBuilding creates and stores the correct effect

    @Test
    void insertBuildingCreatesEffectInTribu() {
        assertTrue(tribu1.getActiveBuildingEffects().isEmpty());

        tribu1.insertBuilding(BUILDING_ID, player1, game);

        List<BuildingEffect> effects = tribu1.getActiveBuildingEffects();
        assertEquals(1, effects.size());
        assertInstanceOf(FlatPrestigeBonusEffect.class, effects.getFirst());

        int insertedPPBuildings = tribu1.getTotalPPBuildings();
        assertEquals(0, insertedPPBuildings);
    }

    // Test 2: Effect activation via direct onPhaseChange

    @Test
    void endGamePhaseTriggersFlatPrestigeBonus() {
        tribu1.insertBuilding(BUILDING_ID, player1, game);

        // Retrieve the concrete effect from Tribu
        PhaseObserver effect = (PhaseObserver) tribu1.getActiveBuildingEffects().getFirst();

        int ppBefore = tribu1.getPrestigePoints();

        // Call directly — same approach as the unit test with mocks,
        // but here Tribu is real, so we check actual PP change
        effect.onPhaseChange(PhaseType.END_GAME);

        int ppAfter = tribu1.getPrestigePoints();
        assertTrue(ppAfter > ppBefore,
                "PP should increase. Before: " + ppBefore + ", After: " + ppAfter);
    }

    // Test 3: Wrong phase does NOT trigger the effect

    @Test
    void nonEndGamePhaseDoesNotTriggerEffect() {
        tribu1.insertBuilding(BUILDING_ID, player1, game);

        PhaseObserver effect = (PhaseObserver) tribu1.getActiveBuildingEffects().get(0);

        int ppBefore = tribu1.getPrestigePoints();

        effect.onPhaseChange(PhaseType.ACTION_RESOLUTION);
        effect.onPhaseChange(PhaseType.END_ROUND);
        effect.onPhaseChange(PhaseType.NEW_ROUND);

        assertEquals(ppBefore, tribu1.getPrestigePoints(),
                "PP should not change for non-END_GAME phases");
    }

    // Test 4: No memory sharing between players

    @Test
    void buildingEffectIsIsolatedToOwner() {
        // 1. Setup: insert the building for player1 and record initial scores
        tribu1.insertBuilding(BUILDING_ID, player1, game);
        int p1Before = tribu1.getPrestigePoints();
        int p2Before = tribu2.getPrestigePoints();

        // 2. Verify that player 2 has NO active effects
        assertTrue(tribu2.getActiveBuildingEffects().isEmpty(),
                "Player 2 should not have any active effects.");

        // 3. Retrieve and trigger player 1's effect
        BuildingEffect effect1 = tribu1.getActiveBuildingEffects().getFirst();
        ((PhaseObserver) effect1).onPhaseChange(PhaseType.END_GAME);

        // 4. Assertions: Player 1 is updated, Player 2 remains unchanged
        assertNotEquals(p1Before, tribu1.getPrestigePoints(),
                "Player 1's score should have changed.");

        assertEquals(p2Before, tribu2.getPrestigePoints(),
                "Player 2's score should NOT have changed.");
    }

}