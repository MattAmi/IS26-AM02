package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.tile.TurnOrderTile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link TurnOrderTile.TurnOrderRewardResult}, verifying the factory for an
 * empty reward result yields zeroed food gain, food penalty and prestige penalty.
 */
class TurnOrderRewardResultTest {

    /** Verifies the empty reward result has all fields set to zero. */
    @Test
    void testEmptyResult() {
        TurnOrderTile.TurnOrderRewardResult emptyResult = TurnOrderTile.TurnOrderRewardResult.empty();
        assertEquals(0, emptyResult.foodGained());
        assertEquals(0, emptyResult.foodPenalty());
        assertEquals(0, emptyResult.ppPenalty());
    }
}
