package it.polimi.ingsw.am02.server.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TurnOrderRewardResultTest {

    @Test
    void testEmptyResult() {
        TurnOrderTile.TurnOrderRewardResult emptyResult = TurnOrderTile.TurnOrderRewardResult.empty();
        assertEquals(0, emptyResult.foodGained());
        assertEquals(0, emptyResult.foodPenalty());
        assertEquals(0, emptyResult.ppPenalty());
    }
}
