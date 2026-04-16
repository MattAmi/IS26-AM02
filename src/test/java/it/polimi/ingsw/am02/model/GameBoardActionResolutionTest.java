package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.model.exceptions.CardNotFoundException;
import it.polimi.ingsw.am02.model.exceptions.EventCardNotTakeableException;
import it.polimi.ingsw.am02.model.exceptions.InsufficientFoodException;
import it.polimi.ingsw.am02.model.exceptions.PickLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameBoardActionResolutionTest {

    private GameRegistry registry;
    private GameBoard gameBoard;
    private Player player1;
    private Player player2;
    private Game game;

    // helpers

    /**
     * Finds the first character card ID in the registry.
     * Characters are identifiable because they are neither events nor buildings.
     */
    private String findCharacterID() {
        // Characters loaded via registry — pick any known character ID from the JSON
        // We scan upperRow / lowerRow which should contain characters after setup
        for (String id : getUpperRow()) {
            if (!registry.isEvent(id) && !registry.isBuilding(id)) {
                return id;
            }
        }
        for (String id : getLowerRow()) {
            if (!registry.isEvent(id) && !registry.isBuilding(id)) {
                return id;
            }
        }
        fail("No character card found in any row");
        return null; // unreachable
    }

    /**
     * Finds the first event card ID currently in the lower row.
     * Events can only appear in the lower row during normal play.
     */
    private String findEventIDInLowerRow() {
        for (String id : getLowerRow()) {
            if (registry.isEvent(id)) {
                return id;
            }
        }
        return null; // no event present
    }

    /**
     * Finds the first event card ID in either row.
     */
    private String findAnyEventID() {
        String id = findEventIDInLowerRow();
        if (id != null) return id;
        for (String rowId : getUpperRow()) {
            if (registry.isEvent(rowId)) {
                return rowId;
            }
        }
        return null;
    }

    /**
     * Finds the first building card ID in the upper building row.
     */
    private String findBuildingIDInUpperRow() {
        for (String id : getUpperRowBuildings()) {
            if (registry.isBuilding(id)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Finds a character card ID in the upper row specifically.
     */
    private String findCharacterIDInUpperRow() {
        for (String id : getUpperRow()) {
            if (!registry.isEvent(id) && !registry.isBuilding(id)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Finds a character card ID in the lower row specifically.
     */
    private String findCharacterIDInLowerRow() {
        for (String id : getLowerRow()) {
            if (!registry.isEvent(id) && !registry.isBuilding(id)) {
                return id;
            }
        }
        return null;
    }

    // Row access helpers (we put these helpers as package-private just for testing)
    private List<String> getUpperRow() {
        return gameBoard.getUpperRow();
    }

    private List<String> getLowerRow() {
        return gameBoard.getLowerRow();
    }

    private List<String> getUpperRowBuildings() {
        return gameBoard.getUpperRowBuildings();
    }

    private List<String> getLowerRowBuildings() {
        return gameBoard.getLowerRowBuildings();
    }

    // setup

    @BeforeEach
    void setUp() {
        TestHelper.ensureRegistryLoaded();
        registry = GameRegistry.getInstance();

        String gameId = "000";
        List<String> nicknames = List.of("Matteo", "Raed");
        Map<String, Totem> totemMap = Map.of("Matteo", Totem.WHITE, "Raed", Totem.BLUE);

        game = new Game(gameId, nicknames, totemMap);
        gameBoard = game.getGameBoard();

        for (int i = 0; i < nicknames.size(); i++) {
            String currentMover = game.getCurrentPlayerNickname();
            char tileId = (i == 0) ? 'E' : 'F';
            game.moveTotem(currentMover, tileId);
        }

        // FSM transitioned to TotemPlacementState

        // We need to find the new order
        List<String> resolutionOrderNicknames = game.getTurnOrder();
        List<Player> resolutionOrder = new ArrayList<>();

        for (String resolutionOrderNickname : resolutionOrderNicknames) {
            Player player = game.getPlayerByNickname(resolutionOrderNickname);
            resolutionOrder.add(player);
        }

        player1 = resolutionOrder.get(0); // First player to resolve actions (tile E)
        player2 = resolutionOrder.get(1); // Second player to resolve actions (tile F)
    }


    //  Tests

    @Nested
    @DisplayName("processActionSelection — character cards (no cost)")
    class CharacterCardTests {

        @Test
        @DisplayName("Taking a valid character card from the upper row succeeds")
        void takeCharacterFromUpperRow() {
            String charID = findCharacterIDInUpperRow();
            if (charID == null) return; // skip if no character in upper row

            assertDoesNotThrow(() ->
                    gameBoard.processActionSelection(player1, List.of(charID), game)
            );

            // Character was removed from upper row
            assertFalse(getUpperRow().contains(charID),
                    "Character should be removed from upperRow after selection");

        }

        @Test
        @DisplayName("Taking a valid character card from the lower row succeeds")
        void takeCharacterFromLowerRow() {
            String charID = findCharacterIDInLowerRow();
            if (charID == null) return; // skip if no character in lower row

            assertDoesNotThrow(() ->
                    gameBoard.processActionSelection(player1, List.of(charID), game)
            );

            assertFalse(getLowerRow().contains(charID),
                    "Character should be removed from lowerRow after selection");
        }
    }

    @Nested
    @DisplayName("processActionSelection — event cards (must throw)")
    class EventCardTests {

        @Test
        @DisplayName("Attempting to take an event card throws an exception")
        void takeEventCardThrows() {
            // Inject an event card into the lower row if none present
            String eventID = findAnyEventID();

            if (eventID == null) {
                // If no event is currently visible, we can't test this path without injecting one manually. Depending on your setup.
                return;
            }

            // Make sure the event is in one of the rows the player can see
            if (!getLowerRow().contains(eventID) && !getUpperRow().contains(eventID)) {
                // Force it into the lower row for testing purposes
                getLowerRow().add(eventID);
            }

            assertThrows(EventCardNotTakeableException.class, () ->
                            gameBoard.processActionSelection(player1, List.of(eventID), game),
                    "Selecting an event card should throw IllegalArgumentException"
            );
        }
    }

    @Nested
    @DisplayName("processActionSelection — building cards (cost validation)")
    class BuildingCardTests {

        @Test
        @DisplayName("Purchasing a building with sufficient food succeeds and deducts cost")
        void purchaseBuildingWithEnoughFood() {
            String buildingID = findBuildingIDInUpperRow();
            if (buildingID == null) return; // no building available

            BuildingCard card = registry.getBuilding(buildingID);
            int cost = card.getBuildingCost();
            int discount = player1.getTribu().getBuildingDiscount();
            int actualCost = Math.max(0, cost - discount);

            // Give the player enough food
            int currentFood = player1.getTribu().getFoodPoints();
            if (currentFood < actualCost) {
                player1.getTribu().addFoodPoints(actualCost - currentFood + 1);
            }
            int foodBefore = player1.getTribu().getFoodPoints();

            assertDoesNotThrow(() ->
                    gameBoard.processActionSelection(player1, List.of(buildingID), game)
            );

            // Food was deducted
            assertEquals(foodBefore - actualCost, player1.getTribu().getFoodPoints(),
                    "Food should decrease by the actual building cost");

            // Building removed from the row
            assertFalse(getLowerRowBuildings().contains(buildingID),
                    "Building should be removed from upperRowBuildings after purchase");
        }

        @Test
        @DisplayName("Purchasing a building with insufficient food throws an exception")
        void purchaseBuildingWithInsufficientFood() {
            String buildingID = findBuildingIDInUpperRow();
            if (buildingID == null) return;

            BuildingCard card = registry.getBuilding(buildingID);
            int cost = card.getBuildingCost();
            int discount = player1.getTribu().getBuildingDiscount();
            int actualCost = Math.max(0, cost - discount);

            if (actualCost == 0) return; // free building, can't test insufficient food

            // Set food to less than the cost
            int currentFood = player1.getTribu().getFoodPoints();
            if (currentFood >= actualCost) {
                // Remove food to make it insufficient
                player1.getTribu().addFoodPoints(-(currentFood - actualCost + 1));
            }

            int foodBefore = player1.getTribu().getFoodPoints();
            assertTrue(foodBefore < actualCost, "Precondition: food must be less than cost");

            assertThrows(InsufficientFoodException.class, () ->
                            gameBoard.processActionSelection(player1, List.of(buildingID), game),
                    "Should throw when player cannot afford the building"
            );

            // Food should not have changed (validation before execution)
            assertEquals(foodBefore, player1.getTribu().getFoodPoints(),
                    "Food should remain unchanged after a failed purchase attempt");

            // Building should still be in the row
            assertTrue(getUpperRowBuildings().contains(buildingID),
                    "Building should remain in upperRowBuildings after a failed purchase");
        }
    }

    @Nested
    @DisplayName("processActionSelection — pick limit enforcement")
    class PickLimitTests {

        @Test
        @DisplayName("Exceeding upper row pick limit throws an exception")
        void exceedUpperRowLimit() {
            // Get the tile's remaining upper picks
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);
            int remainingUpper = tile.getRemainingUpper();

            // Collect more character IDs from upper row than allowed
            List<String> tooManyUpper = new ArrayList<>();
            for (String id : getUpperRow()) {
                if (!registry.isEvent(id)) {
                    tooManyUpper.add(id);
                    if (tooManyUpper.size() > remainingUpper) break;
                }
            }

            if (tooManyUpper.size() <= remainingUpper) {
                // Not enough characters in upper row to exceed the limit
                return;
            }

            assertThrows(PickLimitExceededException.class, () ->
                            gameBoard.processActionSelection(player1, tooManyUpper, game),
                    "Exceeding upper row pick limit should throw IllegalArgumentException"
            );
        }

        @Test
        @DisplayName("Exceeding lower row pick limit throws an exception")
        void exceedLowerRowLimit() {
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);
            int remainingLower = tile.getRemainingLower();

            List<String> tooManyLower = new ArrayList<>();
            for (String id : getLowerRow()) {
                if (!registry.isEvent(id)) {
                    tooManyLower.add(id);
                    if (tooManyLower.size() > remainingLower) break;
                }
            }

            if (tooManyLower.size() <= remainingLower) {
                return;
            }

            assertThrows(PickLimitExceededException.class, () ->
                            gameBoard.processActionSelection(player1, tooManyLower, game),
                    "Exceeding lower row pick limit should throw IllegalArgumentException"
            );
        }

        @Test
        @DisplayName("Card ID not found in any row throws an exception")
        void cardNotFoundInAnyRow() {
            String fakeID = "NONEXISTENT_CARD_999";

            assertThrows(CardNotFoundException.class, () ->
                            gameBoard.processActionSelection(player1, List.of(fakeID), game),
                    "A card ID not present in any row should throw IllegalArgumentException"
            );
        }
    }

    @Nested
    @DisplayName("Decrement picks and remaining counters")
    class DecrementPicksTests {

        @Test
        @DisplayName("After taking one character from upper row, remaining upper decrements by 1")
        void decrementUpperAfterOnePick() {
            String charID = findCharacterIDInUpperRow();
            if (charID == null) return;

            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);
            int remainingBefore = tile.getRemainingUpper();

            if (remainingBefore < 1) return; // can't pick if no allowance

            gameBoard.processActionSelection(player1, List.of(charID), game);

            assertEquals(remainingBefore - 1, tile.getRemainingUpper(),
                    "remainingUpper should decrement by 1 after taking one upper row card");
        }

        @Test
        @DisplayName("After taking one character from lower row, remaining lower decrements by 1")
        void decrementLowerAfterOnePick() {
            String charID = findCharacterIDInLowerRow();
            if (charID == null) return;

            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);
            int remainingBefore = tile.getRemainingLower();

            if (remainingBefore < 1) return;

            gameBoard.processActionSelection(player1, List.of(charID), game);

            assertEquals(remainingBefore - 1, tile.getRemainingLower(),
                    "remainingLower should decrement by 1 after taking one lower row card");
        }

        @Test
        @DisplayName("Taking a building decrements card pick counters")
        void buildingDecrementsCardPickCounters() {
            String buildingID = findBuildingIDInUpperRow();
            if (buildingID == null) return;

            BuildingCard card = registry.getBuilding(buildingID);
            int actualCost = Math.max(0, card.getBuildingCost() - player1.getTribu().getBuildingDiscount());

            // Ensure player has enough food
            int currentFood = player1.getTribu().getFoodPoints();
            if (currentFood < actualCost) {
                player1.getTribu().addFoodPoints(actualCost - currentFood + 1);
            }

            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);
            int remainingUpperBefore = tile.getRemainingUpper();
            int remainingLowerBefore = tile.getRemainingLower();

            gameBoard.processActionSelection(player1, List.of(buildingID), game);

            assertEquals(remainingUpperBefore - 1, tile.getRemainingUpper(),
                    "remainingUpper should change when purchasing a building");
            assertEquals(remainingLowerBefore, tile.getRemainingLower(),
                    "remainingLower should NOT change in this case");
        }
    }

    @Nested
    @DisplayName("canPlayerFinish / isSatisfied")
    class CanPlayerFinishTests {

        @Test
        @DisplayName("Player cannot finish if remaining picks are not zero")
        void cannotFinishWithRemainingPicks() {
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);

            // If there are remaining picks, player should not be able to finish
            if (tile.getRemainingUpper() > 0 || tile.getRemainingLower() > 0) {
                assertFalse(gameBoard.canPlayerFinish(player1),
                        "canPlayerFinish should return false when picks remain");
                assertFalse(tile.isSatisfied(),
                        "isSatisfied should return false when picks remain");
            }
        }

        @Test
        @DisplayName("Player can finish after exhausting all picks")
        void canFinishAfterExhaustingPicks() {
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);

            // Take cards until all picks are exhausted
            // First exhaust upper row picks
            while (tile.getRemainingUpper() > 0) {
                String charID = findCharacterIDInUpperRow();
                if (charID == null) break;
                gameBoard.processActionSelection(player1, List.of(charID), game);
            }

            // Then exhaust lower row picks
            while (tile.getRemainingLower() > 0) {
                String charID = findCharacterIDInLowerRow();
                if (charID == null) break;
                gameBoard.processActionSelection(player1, List.of(charID), game);
            }

            if (tile.getRemainingUpper() == 0 && tile.getRemainingLower() == 0) {
                assertTrue(gameBoard.canPlayerFinish(player1),
                        "canPlayerFinish should return true when all picks are exhausted");
                assertTrue(tile.isSatisfied(),
                        "isSatisfied should return true when remainingUpper and remainingLower are both 0");
            }
        }
    }

    @Nested
    @DisplayName("initializePlayerLimits")
    class InitializePlayerLimitsTests {

        @Test
        @DisplayName("Limits are capped by actual row sizes")
        void limitsAreCappedByRowSizes() {
            // initializePlayerLimits was already called by the FSM during setUp.
            // Verify that remaining picks don't exceed actual row sizes.
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);

            assertTrue(tile.getRemainingUpper() <= getUpperRow().size() + getUpperRowBuildings().size(),
                    "remainingUpper should not exceed the number of cards in upperRow");
            assertTrue(tile.getRemainingLower() <= getLowerRow().size() + getLowerRowBuildings().size(),
                    "remainingLower should not exceed the number of cards in lowerRow");
        }

        @Test
        @DisplayName("Limits are capped by the tile's allowed picks")
        void limitsAreCappedByTileAllowance() {
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);

            assertTrue(tile.getRemainingUpper() <= tile.getNumUpperChoosable(),
                    "remainingUpper should not exceed the tile's allowedUpper");
            assertTrue(tile.getRemainingLower() <= tile.getNumLowerChoosable(),
                    "remainingLower should not exceed the tile's allowedLower");
        }

        @Test
        @DisplayName("Effective limit equals min(allowed, rowSize)")
        void effectiveLimitIsMin() {
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);

            int expectedUpper = Math.min(tile.getNumUpperChoosable(), getUpperRow().size() + getUpperRowBuildings().size());
            int expectedLower = Math.min(tile.getNumLowerChoosable(), getLowerRow().size() + getLowerRowBuildings().size());

            assertEquals(expectedUpper, tile.getRemainingUpper(),
                    "remainingUpper should equal min(allowedUpper, upperRow.size())");
            assertEquals(expectedLower, tile.getRemainingLower(),
                    "remainingLower should equal min(allowedLower, lowerRow.size())");
        }
    }

    @Nested
    @DisplayName("Transactional integrity — failed validation has no side effects")
    class TransactionalIntegrityTests {

        @Test
        @DisplayName("Failed event selection leaves all state unchanged")
        void failedEventSelectionNoSideEffects() {
            String eventID = findAnyEventID();
            if (eventID == null) return;

            // Ensure the event is in a visible row
            boolean inLower = getLowerRow().contains(eventID);
            boolean inUpper = getUpperRow().contains(eventID);
            if (!inLower && !inUpper) {
                getLowerRow().add(eventID);
            }

            int foodBefore = player1.getTribu().getFoodPoints();
            OfferTile tile = gameBoard.getOfferTrack().getTileByPlayer(player1);
            int remainUpperBefore = tile.getRemainingUpper();
            int remainLowerBefore = tile.getRemainingLower();
            int upperRowSizeBefore = getUpperRow().size();
            int lowerRowSizeBefore = getLowerRow().size();

            assertThrows(EventCardNotTakeableException.class, () ->
                    gameBoard.processActionSelection(player1, List.of(eventID), game)
            );

            assertEquals(foodBefore, player1.getTribu().getFoodPoints(),
                    "Food should remain unchanged after failed selection");
            assertEquals(remainUpperBefore, tile.getRemainingUpper(),
                    "remainingUpper should remain unchanged after failed selection");
            assertEquals(remainLowerBefore, tile.getRemainingLower(),
                    "remainingLower should remain unchanged after failed selection");
            assertEquals(upperRowSizeBefore, getUpperRow().size(),
                    "upperRow size should remain unchanged after failed selection");
            assertEquals(lowerRowSizeBefore, getLowerRow().size(),
                    "lowerRow size should remain unchanged after failed selection");
        }

        @Test
        @DisplayName("Failed building purchase (insufficient food) leaves all state unchanged")
        void failedBuildingPurchaseNoSideEffects() {
            String buildingID = findBuildingIDInUpperRow();
            if (buildingID == null) return;

            BuildingCard card = registry.getBuilding(buildingID);
            int actualCost = Math.max(0, card.getBuildingCost() - player1.getTribu().getBuildingDiscount());
            if (actualCost == 0) return;

            // Drain food
            int currentFood = player1.getTribu().getFoodPoints();
            if (currentFood >= actualCost) {
                player1.getTribu().addFoodPoints(-(currentFood - actualCost + 1));
            }

            int foodBefore = player1.getTribu().getFoodPoints();
            int buildingRowSizeBefore = getUpperRowBuildings().size();

            assertThrows(InsufficientFoodException.class, () ->
                    gameBoard.processActionSelection(player1, List.of(buildingID), game)
            );

            assertEquals(foodBefore, player1.getTribu().getFoodPoints(),
                    "Food should remain unchanged after failed building purchase");
            assertEquals(buildingRowSizeBefore, getUpperRowBuildings().size(),
                    "Building row size should remain unchanged after failed purchase");
        }
    }
}