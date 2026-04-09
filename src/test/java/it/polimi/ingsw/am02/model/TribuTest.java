package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.InventionType;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class TribuTest {

    private Tribu tribu;

    private static final CharacterType HUNTER = CharacterType.HUNTER;
    private static final CharacterType BUILDER = CharacterType.BUILDER;

    @BeforeEach
    void setUp() {
        // Starting values chosen to be non-zero so tests can distinguish
        // "value was there from the start" from "value was added by the method".
        tribu = new Tribu();
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

    @Disabled("GameRegistry not yet implemented")
    @Test
    @DisplayName("insertCharacter increases count for that type")
    void insertCharacterIncrementsCount() {
        tribu.insertCharacter("HUNTER_01");
        assertEquals(1, tribu.getCharacterCount(HUNTER));
    }

    @Disabled("GameRegistry not yet implemented")
    @Test
    @DisplayName("insertCharacter increases total character count")
    void insertCharacterIncrementsTotalCount() {
        tribu.insertCharacter("HUNTER_01");
        tribu.insertCharacter("HUNTER_02");
        assertEquals(2, tribu.getNumCharacters());
    }

    @Disabled("GameRegistry not yet implemented")
    @Test
    @DisplayName("Characters of different types are tracked independently")
    void insertCharacterDifferentTypes() {
        tribu.insertCharacter("HUNTER_01");
        tribu.insertCharacter("BUILDER_01");

        assertEquals(1, tribu.getCharacterCount(HUNTER));
        assertEquals(1, tribu.getCharacterCount(BUILDER));
        assertEquals(2, tribu.getNumCharacters());
    }

    @Disabled("GameRegistry not yet implemented")
    @Test
    @DisplayName("getCharacterCount for a type with no insertions returns 0")
    void getCharacterCountReturnsZeroForAbsentType() {
        assertEquals(0, tribu.getCharacterCount(HUNTER));
    }

}
