package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OfferTileTest {

    private OfferTile tileA;
    private OfferTile tileG;
    private Player matteo;
    private Player husnain;

    @BeforeEach
    void setUp() {
        tileA = new OfferTile('A', 5, 3, 0, 0);  // gainedFood = 3
        tileG = new OfferTile('G', 4, 0, 2, 1);  // gainedFood = 0
        matteo = new Player("Matteo", Totem.WHITE);
        husnain = new Player("Husnain", Totem.RED);
    }

    @Test
    @DisplayName("resolveFoodOffer on tile A (Matteo) should credit food exactly once")
    void shouldCreditFoodOnlyOnceTileAMatteo() {
        int initialFood = matteo.getTribu().getFoodPoints();
        tileA.acceptPlayer(matteo);

        // First call assigns food and puts isFoodResolved = true
        tileA.resolveFoodOffer(matteo);
        assertEquals(initialFood + 3, matteo.getTribu().getFoodPoints());

        // Second call doesn't assign food (isFoodResolved = true)
        tileA.resolveFoodOffer(matteo);
        assertEquals(initialFood + 3, matteo.getTribu().getFoodPoints());
    }

    @Test
    @DisplayName("resolveFoodOffer on tile G (Husnain) should credit food exactly once")
    void shouldCreditFoodOnlyOnceTileGHusnain() {
        int initialFood = husnain.getTribu().getFoodPoints();
        tileG.acceptPlayer(husnain);

        // First call: assigns 0 food (tileG), and puts isFoodResolved = true
        tileG.resolveFoodOffer(husnain);
        assertEquals(initialFood, husnain.getTribu().getFoodPoints());

        // Second call doesn't assign food either (isFoodResolved = true)
        tileG.resolveFoodOffer(husnain);
        assertEquals(initialFood, husnain.getTribu().getFoodPoints());
    }

    @Test
    @DisplayName("setRemainingPicks and isSatisfied should work correctly")
    void shouldTrackRemainingPicks() {
        tileA.setRemainingPicks(1, 1);

        assertFalse(tileA.isSatisfied());
        assertEquals(1, tileA.getRemainingUpper());
        assertEquals(1, tileA.getRemainingLower());

        tileA.decrementPicks(1, 0);
        assertFalse(tileA.isSatisfied()); // lower still to be satisfied

        tileA.decrementPicks(0, 1);
        assertTrue(tileA.isSatisfied());  // both at 0
    }

    @Test
    @DisplayName("isSatisfied should return true when both pick quotas start at zero")
    void shouldBeSatisfiedWhenNoPicksRequired() {
        tileA.setRemainingPicks(0, 0);
        assertTrue(tileA.isSatisfied());
    }

    @Test
    @DisplayName("acceptPlayer should assign the player to the tile")
    void shouldAcceptPlayer() {
        tileA.acceptPlayer(matteo);
        assertSame(matteo, tileA.getOccupyingPlayer());
    }

    @Test
    @DisplayName("removePlayer should clear the occupant")
    void shouldRemovePlayer() {
        tileA.acceptPlayer(matteo);
        tileA.removePlayer(matteo);
        assertNull(tileA.getOccupyingPlayer());
    }

    @Test
    @DisplayName("acceptPlayer should silently overwrite the previous occupant (OfferTrack guards occupancy)")
    void shouldOverwriteOccupantSilently() {
        tileA.acceptPlayer(matteo);
        // OfferTrack calls isOccupied() before arriving here:
        // OfferTile does not throw exceptions; it simply performs the assignment.
        tileA.acceptPlayer(husnain);
        assertSame(husnain, tileA.getOccupyingPlayer());
    }

}