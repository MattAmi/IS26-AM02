package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link Tribu}, a player's resource and character container. Covers the
 * default state, the food/prestige/shaman/discount/builder accumulators, and —
 * against the real {@link GameRegistry} — character insertion and its effect on
 * type counts, builder stats, invention tracking and sustenance cost.
 */
class TribuTest {

    private Tribu tribu;
    private static GameRegistry registry;
    private final Player mockPlayer = Mockito.mock(Player.class);

    @BeforeEach
    void setUp() {
        // Starting values chosen to be non-zero so tests can distinguish
        // "value was there from the start" from "value was added by the method".
        tribu = new Tribu();
        TestHelper.ensureRegistryLoaded();
        registry = GameRegistry.getInstance();
        Mockito.when(mockPlayer.getTribu()).thenReturn(tribu);
        Mockito.when(mockPlayer.getNickname()).thenReturn("TestPlayer");
    }

    /** Verifies a new tribù has all counters at zero and flags false. */
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

    /** Verifies a new tribù holds no characters of any type. */
    @Test
    @DisplayName("No characters are present at construction")
    void constructorNoCharacters() {
        assertEquals(0, tribu.getNumCharacters());
        for (CharacterType type : CharacterType.values()) {
            assertEquals(0, tribu.getCharacterCount(type));
        }
    }

    /** Verifies a new tribù tracks no inventions of any type. */
    @Test
    @DisplayName("No inventions are tracked at construction")
    void constructorNoInventions() {
        assertEquals(0, tribu.getNumOfDifferentInventionTypes());
        for (InventionType type : InventionType.values()) {
            assertEquals(0, tribu.getInventionTypeCount(type));
        }
    }

    /** Verifies setFoodPoints overwrites the current food value. */
    @Test
    @DisplayName("setFoodPoints replaces the current value")
    void setFoodPointsReplacesValue() {
        tribu.setFoodPoints(10);
        assertEquals(10, tribu.getFoodPoints());
    }

    /** Verifies addFoodPoints adds a positive delta. */
    @Test
    @DisplayName("addFoodPoints adds a positive delta")
    void addFoodPointsAddsDelta() {
        tribu.addFoodPoints(3);
        assertEquals(3, tribu.getFoodPoints());
    }

    /** Verifies adding zero food leaves the value unchanged. */
    @Test
    @DisplayName("addFoodPoints with zero leaves the value unchanged")
    void addFoodPointsZeroIsNoop() {
        tribu.addFoodPoints(0);
        assertEquals(0, tribu.getFoodPoints());
    }

    /** Verifies a negative delta reduces the food value. */
    @Test
    @DisplayName("addFoodPoints with a negative delta reduces food")
    void addFoodPointsNegativeDelta() {
        tribu.setFoodPoints(5);
        tribu.addFoodPoints(-2);
        assertEquals(3, tribu.getFoodPoints());
    }

    /** Verifies food can be reset to zero. */
    @Test
    @DisplayName("setFoodPoints to zero is valid")
    void setFoodPointsToZero() {
        tribu.setFoodPoints(5);
        tribu.setFoodPoints(0);
        assertEquals(0, tribu.getFoodPoints());
    }

    /** Verifies addPrestigePoints adds to the prestige total. */
    @Test
    @DisplayName("addPrestigePoints accumulates correctly")
    void addPrestigePointsAccumulates() {
        tribu.addPrestigePoints(7);
        assertEquals(7, tribu.getPrestigePoints());
    }

    /** Verifies prestige may go negative. */
    @Test
    @DisplayName("addPrestigePoints with negative value reduces PP (score can go negative)")
    void addPrestigePointsCanGoNegative() {
        tribu.addPrestigePoints(-10);
        assertEquals(-10, tribu.getPrestigePoints());
    }

    /** Verifies repeated prestige additions accumulate. */
    @Test
    @DisplayName("Multiple addPrestigePoints calls are cumulative")
    void addPrestigePointsMultipleCalls() {
        tribu.addPrestigePoints(2);
        tribu.addPrestigePoints(5);
        assertEquals(7, tribu.getPrestigePoints());
    }

    /** Verifies addShamanStars adds to the shaman-star total. */
    @Test
    @DisplayName("addShamanStars accumulates correctly")
    void addShamanStarsAccumulates() {
        tribu.addShamanStars(2);
        assertEquals(2, tribu.getShamanStars());
    }

    /** Verifies repeated shaman-star additions accumulate. */
    @Test
    @DisplayName("addShamanStars multiple times is cumulative")
    void addShamanStarsMultipleCalls() {
        tribu.addShamanStars(1);
        tribu.addShamanStars(3);
        assertEquals(4, tribu.getShamanStars());
    }

    /** Verifies food discounts accumulate across calls. */
    @Test
    @DisplayName("addFoodDiscount accumulates across multiple calls")
    void addFoodDiscountAccumulates() {
        tribu.addFoodDiscount(2);
        tribu.addFoodDiscount(3);
        assertEquals(5, tribu.getFoodDiscount());
    }

    /** Verifies building discounts accumulate without touching the food discount. */
    @Test
    @DisplayName("addBuildingDiscount accumulates independently from food discount")
    void addBuildingDiscountAccumulates() {
        tribu.addBuildingDiscount(1);
        tribu.addBuildingDiscount(4);
        assertEquals(5, tribu.getBuildingDiscount());
        assertEquals(0, tribu.getFoodDiscount());
    }

    /** Verifies food and building discounts are tracked independently. */
    @Test
    @DisplayName("Food and building discounts are independent")
    void discountsAreIndependent() {
        tribu.addFoodDiscount(3);
        tribu.addBuildingDiscount(7);
        assertEquals(3, tribu.getFoodDiscount());
        assertEquals(7, tribu.getBuildingDiscount());
    }

    /** Verifies builder prestige points accumulate. */
    @Test
    @DisplayName("addPPBuilders accumulates correctly")
    void addPPBuildersAccumulates() {
        tribu.addPPBuilders(4);
        tribu.addPPBuilders(2);
        assertEquals(6, tribu.getPPBuilders());
    }


    // With GameRegistry

    /** Verifies inserting a HUNTER raises the hunter count. */
    @Test
    @DisplayName("insertCharacter with a real HUNTER card increments HUNTER count")
    void insertCharacterHunterIncrementsCount() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID, "No HUNTER card found in registry");

        tribu.insertCharacter(hunterID, mockPlayer);
        assertEquals(1, tribu.getCharacterCount(CharacterType.HUNTER));
    }

    /** Verifies inserting a character raises the overall character count. */
    @Test
    @DisplayName("insertCharacter increments total character count")
    void insertCharacterIncrementsTotalCount() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID, "No HUNTER card found in registry");

        tribu.insertCharacter(hunterID, mockPlayer);
        assertEquals(1, tribu.getNumCharacters());
    }

    /** Verifies inserting two of the same type raises both the type and total counts. */
    @Test
    @DisplayName("Multiple insertions of same type increment that type's count")
    void insertMultipleSameTypeCharacters() {
        List<String> hunterIDs = findCardIDsByType(CharacterType.HUNTER, 2);
        assertTrue(hunterIDs.size() >= 2, "Need at least 2 HUNTER cards in registry");

        tribu.insertCharacter(hunterIDs.get(0), mockPlayer);
        tribu.insertCharacter(hunterIDs.get(1), mockPlayer);

        assertEquals(2, tribu.getCharacterCount(CharacterType.HUNTER));
        assertEquals(2, tribu.getNumCharacters());
    }

    /** Verifies different character types are counted independently. */
    @Test
    @DisplayName("Characters of different types are tracked independently")
    void insertDifferentTypeCharacters() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        String builderID = findFirstCardIDByType(CharacterType.BUILDER);
        assertNotNull(hunterID);
        assertNotNull(builderID);

        tribu.insertCharacter(hunterID, mockPlayer);
        tribu.insertCharacter(builderID, mockPlayer);

        assertEquals(1, tribu.getCharacterCount(CharacterType.HUNTER));
        assertEquals(1, tribu.getCharacterCount(CharacterType.BUILDER));
        assertEquals(2, tribu.getNumCharacters());
    }

    /** Verifies an uninserted character type has a count of zero. */
    @Test
    @DisplayName("getCharacterCount returns 0 for type with no insertions after registry load")
    void getCharacterCountZeroForAbsentType() {
        assertEquals(0, tribu.getCharacterCount(CharacterType.SHAMAN));
    }

    /** Verifies inserting a BUILDER raises the building discount. */
    @Test
    @DisplayName("Inserting a BUILDER increases buildingDiscount")
    void insertBuilderIncreasesBuildingDiscount() {
        String builderID = findFirstCardIDByType(CharacterType.BUILDER);
        assertNotNull(builderID, "No BUILDER card found in registry");

        int discountBefore = tribu.getBuildingDiscount();
        tribu.insertCharacter(builderID, mockPlayer);

        assertTrue(tribu.getBuildingDiscount() > discountBefore,
                "Building discount should increase after inserting a BUILDER");
    }

    /** Verifies the building discount never decreases as more BUILDERs are added. */
    @Test
    @DisplayName("Inserting multiple BUILDERs accumulates buildingDiscount")
    void insertMultipleBuildersAccumulatesDiscount() {
        List<String> builderIDs = findCardIDsByType(CharacterType.BUILDER, 2);
        assertTrue(builderIDs.size() >= 2, "Need at least 2 BUILDER cards in registry");

        tribu.insertCharacter(builderIDs.get(0), mockPlayer);
        int discountAfterFirst = tribu.getBuildingDiscount();

        tribu.insertCharacter(builderIDs.get(1), mockPlayer);
        assertTrue(tribu.getBuildingDiscount() >= discountAfterFirst,
                "Building discount should not decrease after adding another BUILDER");
    }

    /** Verifies inserting a BUILDER raises the builder prestige points. */
    @Test
    @DisplayName("Inserting a BUILDER updates totalPPBuilders")
    void insertBuilderUpdatesPPBuilders() {
        String builderID = findFirstCardIDByType(CharacterType.BUILDER);
        assertNotNull(builderID);

        tribu.insertCharacter(builderID, mockPlayer);

        assertTrue(tribu.getPPBuilders() > 0,
                "PP from builders should be positive after inserting a BUILDER");
    }

    /** Verifies inserting an INVENTOR starts tracking an invention type. */
    @Test
    @DisplayName("Inserting an INVENTOR updates inventionCounts")
    void insertInventorUpdatesInventionCounts() {
        String inventorID = findFirstCardIDByType(CharacterType.INVENTOR);
        assertNotNull(inventorID, "No INVENTOR card found in registry");

        tribu.insertCharacter(inventorID, mockPlayer);

        assertTrue(tribu.getNumOfDifferentInventionTypes() > 0,
                "After inserting an INVENTOR, at least one invention type should be tracked");
    }

    /** Verifies the distinct invention count never decreases as INVENTORs are added. */
    @Test
    @DisplayName("Inserting INVENTORs with different inventions increases distinct count")
    void insertInventorsWithDifferentInventions() {
        // Find two inventors with different invention types
        List<String> inventorIDs = findCardIDsByType(CharacterType.INVENTOR, 6);
        assertTrue(inventorIDs.size() >= 2, "Need at least 2 INVENTOR cards in registry");

        tribu.insertCharacter(inventorIDs.getFirst(), mockPlayer);
        int distinctAfterFirst = tribu.getNumOfDifferentInventionTypes();

        // Try adding more inventors until we find one that increases distinct count
        for (int i = 1; i < inventorIDs.size(); i++) {
            tribu.insertCharacter(inventorIDs.get(i), mockPlayer);
        }

        assertTrue(tribu.getNumOfDifferentInventionTypes() >= distinctAfterFirst,
                "Adding more INVENTORs should not decrease distinct invention count");
    }

    /** Verifies a non-BUILDER character leaves builder discount and PP untouched. */
    @Test
    @DisplayName("Inserting a HUNTER does not change buildingDiscount or PPBuilders")
    void insertNonBuilderDoesNotAffectBuilderStats() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID);

        tribu.insertCharacter(hunterID, mockPlayer);

        assertEquals(0, tribu.getBuildingDiscount(),
                "HUNTER should not affect building discount");
        assertEquals(0, tribu.getPPBuilders(),
                "HUNTER should not affect PP from builders");
    }

    /** Verifies sustenance cost is zero for an empty tribù. */
    @Test
    @DisplayName("Sustenance cost is 0 when tribe has no characters")
    void sustenanceCostZeroWithNoCharacters() {
        assertEquals(0, tribu.calculateSustenanceCost());
    }

    /** Verifies sustenance cost becomes positive after adding a character. */
    @Test
    @DisplayName("Sustenance cost increases after inserting characters")
    void sustenanceCostIncreasesWithCharacters() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID);

        tribu.insertCharacter(hunterID, mockPlayer);

        assertTrue(tribu.calculateSustenanceCost() > 0,
                "Sustenance cost should be positive after adding a character");
    }

    /** Verifies sustenance cost grows monotonically with the character count. */
    @Test
    @DisplayName("Sustenance cost scales with the number of characters")
    void sustenanceCostScalesWithCharacterCount() {
        List<String> hunterIDs = findCardIDsByType(CharacterType.HUNTER, 3);
        assertTrue(hunterIDs.size() >= 3, "Need at least 3 HUNTER cards in registry");

        tribu.insertCharacter(hunterIDs.get(0), mockPlayer);
        int costAfter1 = tribu.calculateSustenanceCost();

        tribu.insertCharacter(hunterIDs.get(1), mockPlayer);
        int costAfter2 = tribu.calculateSustenanceCost();

        tribu.insertCharacter(hunterIDs.get(2), mockPlayer);
        int costAfter3 = tribu.calculateSustenanceCost();

        assertTrue(costAfter2 > costAfter1,
                "Cost after 2 characters should exceed cost after 1");
        assertTrue(costAfter3 > costAfter2,
                "Cost after 3 characters should exceed cost after 2");
    }

    /** Verifies building discount is non-decreasing and positive across several BUILDERs. */
    @Test
    @DisplayName("getBuildingDiscount reflects accumulated BUILDER discounts correctly")
    void buildingDiscountReflectsAllBuilders() {
        List<String> builderIDs = findCardIDsByType(CharacterType.BUILDER, 3);
        assertTrue(builderIDs.size() >= 3, "Need at least 3 BUILDER cards in registry");

        int previousDiscount = 0;
        for (String id : builderIDs) {
            tribu.insertCharacter(id, mockPlayer);
            int currentDiscount = tribu.getBuildingDiscount();
            assertTrue(currentDiscount >= previousDiscount,
                    "Building discount should never decrease after inserting a BUILDER (was "
                            + previousDiscount + ", now " + currentDiscount + ")");
            previousDiscount = currentDiscount;
        }

        assertTrue(tribu.getBuildingDiscount() > 0,
                "After inserting 3 BUILDERs, total building discount should be positive");
    }

    /** Verifies builder prestige points are non-decreasing and positive across several BUILDERs. */
    @Test
    @DisplayName("getPPBuilders reflects accumulated BUILDER PP correctly")
    void ppBuildersReflectsAllBuilders() {
        List<String> builderIDs = findCardIDsByType(CharacterType.BUILDER, 3);
        assertTrue(builderIDs.size() >= 3, "Need at least 3 BUILDER cards in registry");

        int previousPP = 0;
        for (String id : builderIDs) {
            tribu.insertCharacter(id, mockPlayer);
            int currentPP = tribu.getPPBuilders();
            assertTrue(currentPP >= previousPP,
                    "PP from builders should never decrease after inserting a BUILDER (was "
                            + previousPP + ", now " + currentPP + ")");
            previousPP = currentPP;
        }

        assertTrue(tribu.getPPBuilders() > 0,
                "After inserting 3 BUILDERs, total PP from builders should be positive");
    }

    /** Verifies building prestige points are unaffected by inserting characters. */
    @Test
    @DisplayName("totalPPBuildings starts at 0 and is unaffected by character insertions")
    void totalPPBuildingsUnchangedByCharacters() {
        String hunterID = findFirstCardIDByType(CharacterType.HUNTER);
        assertNotNull(hunterID);

        tribu.insertCharacter(hunterID, mockPlayer);
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
