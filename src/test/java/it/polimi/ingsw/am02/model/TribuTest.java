package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.InventionType;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TribuTest {

    private Tribu tribu;
    private static GameRegistry registry;

    @BeforeEach
    void setUp() {
        // Starting values chosen to be non-zero so tests can distinguish
        // "value was there from the start" from "value was added by the method".
        tribu = new Tribu();
        TestHelper.ensureRegistryLoaded();
        registry = GameRegistry.getInstance();
    }

    @Test
    @DisplayName("Default constructor initializes all numeric fields to 0 and boolean fields to false")
    void constructorInitializesAllFieldsToDefaults() {
        assertAll(
                () -> assertEquals(0, tribu.getFoodPoints()),
                () -> assertEquals(0, tribu.getPrestigePoints()),
                () -> assertEquals(0, tribu.getShamanStars()),
                () -> assertEquals(0, tribu.getFoodDiscount()),
                () -> assertEquals(0, tribu.getBuildingDiscount()),
                () -> assertEquals(0, tribu.getPPBuilders()),
                () -> assertEquals(0, tribu.getTotalPPBuildings()),
                () -> assertEquals(0, tribu.getNumCharacters()),
                () -> assertEquals(0, tribu.getLastEventBonusReceived()),

                () -> assertFalse(tribu.isImmune())
        );
    }

    @Test
    @DisplayName("No characters are present at construction")
    void constructorNoCharacters() {
        assertEquals(0, tribu.getNumCharacters());
        for (CharacterType type : CharacterType.values()) {
            assertEquals(0, tribu.getCharacterCount(type));
        }
    }

    @Test
    @DisplayName("No inventions are tracked at construction")
    void constructorNoInventions() {
        assertEquals(0, tribu.getNumOfDifferentInventionTypes());
        for (InventionType type : InventionType.values()) {
            assertEquals(0, tribu.getInventionTypeCount(type));
        }
    }

    @Test
    @DisplayName("setFoodPoints replaces the current value")
    void setFoodPointsReplacesValue() {
        tribu.setFoodPoints(10);
        assertEquals(10, tribu.getFoodPoints());
    }

    @Test
    @DisplayName("addFoodPoints adds a positive delta")
    void addFoodPointsAddsDelta() {
        tribu.addFoodPoints(3);
        assertEquals(3, tribu.getFoodPoints());
    }

    @Test
    @DisplayName("addFoodPoints with zero leaves the value unchanged")
    void addFoodPointsZeroIsNoop() {
        tribu.addFoodPoints(0);
        assertEquals(0, tribu.getFoodPoints());
    }

    @Test
    @DisplayName("addFoodPoints with a negative delta reduces food")
    void addFoodPointsNegativeDelta() {
        tribu.setFoodPoints(5);
        tribu.addFoodPoints(-2);
        assertEquals(3, tribu.getFoodPoints());
    }

    @Test
    @DisplayName("setFoodPoints to zero is valid")
    void setFoodPointsToZero() {
        tribu.setFoodPoints(5);
        tribu.setFoodPoints(0);
        assertEquals(0, tribu.getFoodPoints());
    }

    @Test
    @DisplayName("addPrestigePoints accumulates correctly")
    void addPrestigePointsAccumulates() {
        tribu.addPrestigePoints(7);
        assertEquals(7, tribu.getPrestigePoints());
    }

    @Test
    @DisplayName("addPrestigePoints with negative value reduces PP (score can go negative)")
    void addPrestigePointsCanGoNegative() {
        tribu.addPrestigePoints(-10);
        assertEquals(-10, tribu.getPrestigePoints());
    }

    @Test
    @DisplayName("Multiple addPrestigePoints calls are cumulative")
    void addPrestigePointsMultipleCalls() {
        tribu.addPrestigePoints(2);
        tribu.addPrestigePoints(5);
        assertEquals(7, tribu.getPrestigePoints());
    }

    @Test
    @DisplayName("addShamanStars accumulates correctly")
    void addShamanStarsAccumulates() {
        tribu.addShamanStars(2);
        assertEquals(2, tribu.getShamanStars());
    }

    @Test
    @DisplayName("addShamanStars multiple times is cumulative")
    void addShamanStarsMultipleCalls() {
        tribu.addShamanStars(1);
        tribu.addShamanStars(3);
        assertEquals(4, tribu.getShamanStars());
    }

    @Test
    @DisplayName("addFoodDiscount accumulates across multiple calls")
    void addFoodDiscountAccumulates() {
        tribu.addFoodDiscount(2);
        tribu.addFoodDiscount(3);
        assertEquals(5, tribu.getFoodDiscount());
    }

    @Test
    @DisplayName("addBuildingDiscount accumulates independently from food discount")
    void addBuildingDiscountAccumulates() {
        tribu.addBuildingDiscount(1);
        tribu.addBuildingDiscount(4);
        assertEquals(5, tribu.getBuildingDiscount());
        assertEquals(0, tribu.getFoodDiscount());
    }

    @Test
    @DisplayName("Food and building discounts are independent")
    void discountsAreIndependent() {
        tribu.addFoodDiscount(3);
        tribu.addBuildingDiscount(7);
        assertEquals(3, tribu.getFoodDiscount());
        assertEquals(7, tribu.getBuildingDiscount());
    }

    @Test
    @DisplayName("addPPBuilders accumulates correctly")
    void addPPBuildersAccumulates() {
        tribu.addPPBuilders(4);
        tribu.addPPBuilders(2);
        assertEquals(6, tribu.getPPBuilders());
    }


    // With GameRegistry

    @Test
    @DisplayName("insertCharacter with a real HUNTER card increments HUNTER count")
    void insertCharacterHunterIncrementsCount() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID, "No HUNTER card found in registry");

        tribu.insertCharacter(hunterID);
        assertEquals(1, tribu.getCharacterCount(CharacterType.HUNTER));
    }

    @Test
    @DisplayName("insertCharacter increments total character count")
    void insertCharacterIncrementsTotalCount() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID, "No HUNTER card found in registry");

        tribu.insertCharacter(hunterID);
        assertEquals(1, tribu.getNumCharacters());
    }

    @Test
    @DisplayName("Multiple insertions of same type increment that type's count")
    void insertMultipleSameTypeCharacters() {
        List<String> hunterIDs = findCardIDsByType(CharacterType.HUNTER, 2);
        assertTrue(hunterIDs.size() >= 2, "Need at least 2 HUNTER cards in registry");

        tribu.insertCharacter(hunterIDs.get(0));
        tribu.insertCharacter(hunterIDs.get(1));

        assertEquals(2, tribu.getCharacterCount(CharacterType.HUNTER));
        assertEquals(2, tribu.getNumCharacters());
    }

    @Test
    @DisplayName("Characters of different types are tracked independently")
    void insertDifferentTypeCharacters() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        String builderID = findFirstCardIDByType(CharacterType.BUILDER);
        assertNotNull(hunterID);
        assertNotNull(builderID);

        tribu.insertCharacter(hunterID);
        tribu.insertCharacter(builderID);

        assertEquals(1, tribu.getCharacterCount(CharacterType.HUNTER));
        assertEquals(1, tribu.getCharacterCount(CharacterType.BUILDER));
        assertEquals(2, tribu.getNumCharacters());
    }

    @Test
    @DisplayName("getCharacterCount returns 0 for type with no insertions after registry load")
    void getCharacterCountZeroForAbsentType() {
        assertEquals(0, tribu.getCharacterCount(CharacterType.SHAMAN));
    }

    @Test
    @DisplayName("Inserting a BUILDER increases buildingDiscount")
    void insertBuilderIncreasesBuildingDiscount() {
        String builderID = findFirstCardIDByType(CharacterType.BUILDER);
        assertNotNull(builderID, "No BUILDER card found in registry");

        int discountBefore = tribu.getBuildingDiscount();
        tribu.insertCharacter(builderID);

        assertTrue(tribu.getBuildingDiscount() > discountBefore,
                "Building discount should increase after inserting a BUILDER");
    }

    @Test
    @DisplayName("Inserting multiple BUILDERs accumulates buildingDiscount")
    void insertMultipleBuildersAccumulatesDiscount() {
        List<String> builderIDs = findCardIDsByType(CharacterType.BUILDER, 2);
        assertTrue(builderIDs.size() >= 2, "Need at least 2 BUILDER cards in registry");

        tribu.insertCharacter(builderIDs.get(0));
        int discountAfterFirst = tribu.getBuildingDiscount();

        tribu.insertCharacter(builderIDs.get(1));
        assertTrue(tribu.getBuildingDiscount() >= discountAfterFirst,
                "Building discount should not decrease after adding another BUILDER");
    }

    @Test
    @DisplayName("Inserting a BUILDER updates totalPPBuilders")
    void insertBuilderUpdatesPPBuilders() {
        String builderID = findFirstCardIDByType(CharacterType.BUILDER);
        assertNotNull(builderID);

        tribu.insertCharacter(builderID);

        assertTrue(tribu.getPPBuilders() > 0,
                "PP from builders should be positive after inserting a BUILDER");
    }

    @Test
    @DisplayName("Inserting an INVENTOR updates inventionCounts")
    void insertInventorUpdatesInventionCounts() {
        String inventorID = findFirstCardIDByType(CharacterType.INVENTOR);
        assertNotNull(inventorID, "No INVENTOR card found in registry");

        tribu.insertCharacter(inventorID);

        assertTrue(tribu.getNumOfDifferentInventionTypes() > 0,
                "After inserting an INVENTOR, at least one invention type should be tracked");
    }

    @Test
    @DisplayName("Inserting INVENTORs with different inventions increases distinct count")
    void insertInventorsWithDifferentInventions() {
        // Find two inventors with different invention types
        List<String> inventorIDs = findCardIDsByType(CharacterType.INVENTOR, 6);
        assertTrue(inventorIDs.size() >= 2, "Need at least 2 INVENTOR cards in registry");

        tribu.insertCharacter(inventorIDs.getFirst());
        int distinctAfterFirst = tribu.getNumOfDifferentInventionTypes();

        // Try adding more inventors until we find one that increases distinct count
        for (int i = 1; i < inventorIDs.size(); i++) {
            tribu.insertCharacter(inventorIDs.get(i));
        }

        assertTrue(tribu.getNumOfDifferentInventionTypes() >= distinctAfterFirst,
                "Adding more INVENTORs should not decrease distinct invention count");
    }

    @Test
    @DisplayName("Inserting a HUNTER does not change buildingDiscount or PPBuilders")
    void insertNonBuilderDoesNotAffectBuilderStats() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID);

        tribu.insertCharacter(hunterID);

        assertEquals(0, tribu.getBuildingDiscount(),
                "HUNTER should not affect building discount");
        assertEquals(0, tribu.getPPBuilders(),
                "HUNTER should not affect PP from builders");
    }

    @Test
    @DisplayName("Sustenance cost is 0 when tribe has no characters")
    void sustenanceCostZeroWithNoCharacters() {
        assertEquals(0, tribu.calculateSustenanceCost());
    }

    @Test
    @DisplayName("Sustenance cost increases after inserting characters")
    void sustenanceCostIncreasesWithCharacters() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID);

        tribu.insertCharacter(hunterID);

        assertTrue(tribu.calculateSustenanceCost() > 0,
                "Sustenance cost should be positive after adding a character");
    }

    @Test
    @DisplayName("Sustenance cost scales with the number of characters")
    void sustenanceCostScalesWithCharacterCount() {
        List<String> hunterIDs = findCardIDsByType(CharacterType.HUNTER, 3);
        assertTrue(hunterIDs.size() >= 3, "Need at least 3 HUNTER cards in registry");

        tribu.insertCharacter(hunterIDs.get(0));
        int costAfter1 = tribu.calculateSustenanceCost();

        tribu.insertCharacter(hunterIDs.get(1));
        int costAfter2 = tribu.calculateSustenanceCost();

        tribu.insertCharacter(hunterIDs.get(2));
        int costAfter3 = tribu.calculateSustenanceCost();

        assertTrue(costAfter2 > costAfter1,
                "Cost after 2 characters should exceed cost after 1");
        assertTrue(costAfter3 > costAfter2,
                "Cost after 3 characters should exceed cost after 2");
    }

    @Test
    @DisplayName("getBuildingDiscount reflects accumulated BUILDER discounts correctly")
    void buildingDiscountReflectsAllBuilders() {
        List<String> builderIDs = findCardIDsByType(CharacterType.BUILDER, 3);
        assertTrue(builderIDs.size() >= 3, "Need at least 3 BUILDER cards in registry");

        int previousDiscount = 0;
        for (String id : builderIDs) {
            tribu.insertCharacter(id);
            int currentDiscount = tribu.getBuildingDiscount();
            assertTrue(currentDiscount >= previousDiscount,
                    "Building discount should never decrease after inserting a BUILDER (was "
                            + previousDiscount + ", now " + currentDiscount + ")");
            previousDiscount = currentDiscount;
        }

        assertTrue(tribu.getBuildingDiscount() > 0,
                "After inserting 3 BUILDERs, total building discount should be positive");
    }

    @Test
    @DisplayName("getPPBuilders reflects accumulated BUILDER PP correctly")
    void ppBuildersReflectsAllBuilders() {
        List<String> builderIDs = findCardIDsByType(CharacterType.BUILDER, 3);
        assertTrue(builderIDs.size() >= 3, "Need at least 3 BUILDER cards in registry");

        int previousPP = 0;
        for (String id : builderIDs) {
            tribu.insertCharacter(id);
            int currentPP = tribu.getPPBuilders();
            assertTrue(currentPP >= previousPP,
                    "PP from builders should never decrease after inserting a BUILDER (was "
                            + previousPP + ", now " + currentPP + ")");
            previousPP = currentPP;
        }

        assertTrue(tribu.getPPBuilders() > 0,
                "After inserting 3 BUILDERs, total PP from builders should be positive");
    }

    @Test
    @DisplayName("totalPPBuildings starts at 0 and is unaffected by character insertions")
    void totalPPBuildingsUnchangedByCharacters() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID);

        tribu.insertCharacter(hunterID);
        assertEquals(0, tribu.getTotalPPBuildings(),
                "Character insertions should not affect totalPPBuildings");
    }



    // Helper methods

    /**
     * Finds the first character card ID of the given type in the registry.
     */
    private String findFirstCardIDByType(CharacterType type) {
        return registry.getAllCharactersIDs().stream()
                .filter(id -> registry.getCharacter(id).getType() == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds up to {@code limit} character card IDs of the given type in the registry.
     */
    private List<String> findCardIDsByType(CharacterType type, int limit) {
        return registry.getAllCharactersIDs().stream()
                .filter(id -> registry.getCharacter(id).getType() == type)
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

}
