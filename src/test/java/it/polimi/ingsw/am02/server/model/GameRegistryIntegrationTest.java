package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GameRegistry loaded with real JSON data via TestHelper.
 * Validates that all JSON files are parsed correctly and that the registry
 * exposes consistent, complete data for characters, events, buildings,
 * offer tiles, and turn order tiles.
 * No mocking is used: the real GameRegistry singleton is loaded once
 * with all JSON files and shared across all tests.
 */
class GameRegistryIntegrationTest {

    private static GameRegistry registry;

    @BeforeAll
    static void setUp() {
        TestHelper.ensureRegistryLoaded();
        registry = GameRegistry.getInstance();
    }


    // 1. CHARACTER CARDS

    @Nested
    @DisplayName("Character cards loading")
    class CharacterCardTests {

        /**
         * Verifies that all 84 character cards from the JSON are loaded.
         * Count: 29 (Era I) + 28 (Era II) + 27 (Era III) = 84
         */
        @Test
        @DisplayName("All 84 character cards are loaded")
        void allCharactersLoaded() {
            // The JSON has C_001 through C_084
            for (int i = 1; i <= 84; i++) {
                String cardID = String.format("C_%03d", i);
                CharacterCard card = registry.getCharacter(cardID);
                assertNotNull(card, "Character card " + cardID + " should be in registry");
            }
        }

        /**
         * Every character card must have a non-null, non-empty ID,
         * a valid Era, a valid CharacterType, and minPlayers >= 2.
         */
        @Test
        @DisplayName("Every character card has valid core fields")
        void characterFieldsAreValid() {
            for (int i = 1; i <= 84; i++) {
                String cardID = String.format("C_%03d", i);
                CharacterCard card = registry.getCharacter(cardID);

                assertNotNull(card.getID(), cardID + " ID should not be null");
                assertFalse(card.getID().isEmpty(), cardID + " ID should not be empty");
                assertNotNull(card.getEra(), cardID + " era should not be null");
                assertNotNull(card.getType(), cardID + " type should not be null");
                assertTrue(card.getMinPlayers() >= 2, cardID + " minPlayers should be >= 2");
                assertTrue(card.getMinPlayers() <= 5, cardID + " minPlayers should be <= 5");
            }
        }

        /**
         * Verifies the correct distribution of character types per era,
         * filtered for 2-player games (minPlayers <= 2).
         */
        @Test
        @DisplayName("Era I 2-player characters: type distribution is correct")
        void eraOneTypeCounts() {
            // From the JSON, Era I with minPlayers <= 2:
            // INVENTOR: C_001...C_004 = 4
            // BUILDER:  C_008...C_010 = 3
            // GATHERER: C_012, C_013 = 2
            // ARTIST:   C_016...C_018 = 3
            // SHAMAN:   C_021, C_022 = 2
            // HUNTER:   C_025...C_027 = 3
            Map<CharacterType, Integer> counts = new EnumMap<>(CharacterType.class);
            for (int i = 1; i <= 29; i++) {
                String cardID = String.format("C_%03d", i);
                CharacterCard card = registry.getCharacter(cardID);
                if (card.getEra() == Era.I && card.getMinPlayers() <= 2) {
                    counts.merge(card.getType(), 1, Integer::sum);
                }
            }
            assertEquals(4, counts.getOrDefault(CharacterType.INVENTOR, 0));
            assertEquals(3, counts.getOrDefault(CharacterType.BUILDER, 0));
            assertEquals(2, counts.getOrDefault(CharacterType.GATHERER, 0));
            assertEquals(3, counts.getOrDefault(CharacterType.ARTIST, 0));
            assertEquals(2, counts.getOrDefault(CharacterType.SHAMAN, 0));
            assertEquals(3, counts.getOrDefault(CharacterType.HUNTER, 0));
        }

        /**
         * All 6 character types must be present across the full card set.
         */
        @Test
        @DisplayName("All 6 character types are represented")
        void allTypesPresent() {
            Set<CharacterType> foundTypes = EnumSet.noneOf(CharacterType.class);
            for (int i = 1; i <= 84; i++) {
                String cardID = String.format("C_%03d", i);
                foundTypes.add(registry.getCharacter(cardID).getType());
            }
            assertEquals(EnumSet.allOf(CharacterType.class), foundTypes);
        }

        /**
         * All 3 eras must be present across the full card set.
         */
        @Test
        @DisplayName("All 3 eras are represented in characters")
        void allErasPresent() {
            Set<Era> foundEras = EnumSet.noneOf(Era.class);
            for (int i = 1; i <= 84; i++) {
                String cardID = String.format("C_%03d", i);
                foundEras.add(registry.getCharacter(cardID).getEra());
            }
            assertTrue(foundEras.contains(Era.I));
            assertTrue(foundEras.contains(Era.II));
            assertTrue(foundEras.contains(Era.III));
        }

        /**
         * Spot-checks specific character cards against the JSON to verify
         * that the factory correctly wires type, era, and minPlayers.
         */
        @Test
        @DisplayName("Spot check: C_001 is Era I INVENTOR with minPlayers 2")
        void spotCheckC001() {
            CharacterCard card = registry.getCharacter("C_001");
            assertEquals(Era.I, card.getEra());
            assertEquals(CharacterType.INVENTOR, card.getType());
            assertEquals(2, card.getMinPlayers());
        }

        @Test
        @DisplayName("Spot check: C_011 is Era I BUILDER with minPlayers 5")
        void spotCheckC011() {
            CharacterCard card = registry.getCharacter("C_011");
            assertEquals(Era.I, card.getEra());
            assertEquals(CharacterType.BUILDER, card.getType());
            assertEquals(5, card.getMinPlayers());
        }

        @Test
        @DisplayName("Spot check: C_059 is Era III INVENTOR")
        void spotCheckC059() {
            CharacterCard card = registry.getCharacter("C_059");
            assertEquals(Era.III, card.getEra());
            assertEquals(CharacterType.INVENTOR, card.getType());
        }

//        /**
//         * Every character card must have a non-null CharacterEffect.
//         * The factory should never produce a card without an effect.
//         */
//        @Disabled
//        @Test
//        @DisplayName("Every character card has a non-null effect")
//        void allCharactersHaveEffect() {
//            for (int i = 1; i <= 84; i++) {
//                String cardID = String.format("C_%03d", i);
//                CharacterCard card = registry.getCharacter(cardID);
//                assertNotNull(card.getCharacterEffect(), cardID + " should have a non-null effect"); // we don't have a getCharacterEffect
//            }
//        }

        /**
         * A non-existent card ID should return null from the registry.
         */
        @Test
        @DisplayName("Non-existent character ID returns null")
        void nonExistentCharacterReturnsNull() {
            assertNull(registry.getCharacter("C_999"));
            assertNull(registry.getCharacter("FAKE"));
        }
    }


    // 2. EVENT CARDS

    @Nested
    @DisplayName("Event cards loading")
    class EventCardTests {

        /**
         * All 12 events from the JSON must be loaded (E_001...E_012).
         */
        @Test
        @DisplayName("All 12 event cards are loaded")
        void allEventsLoaded() {
            for (int i = 1; i <= 12; i++) {
                String cardID = String.format("E_%03d", i);
                EventCard card = registry.getEvent(cardID);
                assertNotNull(card, "Event card " + cardID + " should be in registry");
            }
        }

        /**
         * Every event card must have valid fields: non-null ID, era, type,
         * and priority > 0.
         */
        @Test
        @DisplayName("Every event card has valid core fields")
        void eventFieldsAreValid() {
            for (int i = 1; i <= 12; i++) {
                String cardID = String.format("E_%03d", i);
                EventCard card = registry.getEvent(cardID);

                assertNotNull(card.getID(), cardID + " ID should not be null");
                assertNotNull(card.getEra(), cardID + " era should not be null");
                assertNotNull(card.getType(), cardID + " type should not be null");
                assertTrue(card.getPriority() > 0, cardID + " priority should be > 0");
            }
        }

        /**
         * All 4 event types must be present.
         */
        @Test
        @DisplayName("All 4 event types are represented")
        void allEventTypesPresent() {
            Set<EventType> foundTypes = EnumSet.noneOf(EventType.class);
            for (int i = 1; i <= 12; i++) {
                String cardID = String.format("E_%03d", i);
                foundTypes.add(registry.getEvent(cardID).getType());
            }
            assertEquals(EnumSet.allOf(EventType.class), foundTypes);
        }

        /**
         * Exactly 2 events must be marked as final (isFinal == true).
         * From the JSON: E_011 (SUSTENANCE final) and E_012 (SHAMANIC_RITUAL final).
         */
        @Test
        @DisplayName("Exactly 2 final events exist")
        void twoFinalEvents() {
            int finalCount = 0;
            for (int i = 1; i <= 12; i++) {
                String cardID = String.format("E_%03d", i);
                if (registry.getEvent(cardID).isFinal()) {
                    finalCount++;
                }
            }
            assertEquals(2, finalCount);
        }

        /**
         * Final events should be Era III only.
         */
        @Test
        @DisplayName("Final events are all Era III")
        void finalEventsAreEraThree() {
            for (int i = 1; i <= 12; i++) {
                String cardID = String.format("E_%03d", i);
                EventCard card = registry.getEvent(cardID);
                if (card.isFinal()) {
                    assertEquals(Era.III, card.getEra(),
                            cardID + " is final but not Era III");
                }
            }
        }

        /**
         * Spot check: E_002 is Era I SUSTENANCE, not final, priority 5.
         */
        @Test
        @DisplayName("Spot check: E_002 is Era I SUSTENANCE, priority 5, not final")
        void spotCheckE002() {
            EventCard card = registry.getEvent("E_002");
            assertEquals(Era.I, card.getEra());
            assertEquals(EventType.SUSTENANCE, card.getType());
            assertEquals(5, card.getPriority());
            assertFalse(card.isFinal());
        }

        /**
         * Spot check: E_011 is Era III SUSTENANCE final, priority 100.
         */
        @Test
        @DisplayName("Spot check: E_011 is Era III SUSTENANCE final, priority 100")
        void spotCheckE011() {
            EventCard card = registry.getEvent("E_011");
            assertEquals(Era.III, card.getEra());
            assertEquals(EventType.SUSTENANCE, card.getType());
            assertEquals(100, card.getPriority());
            assertTrue(card.isFinal());
        }

        /**
         * Spot check: E_012 is Era III SHAMANIC_RITUAL final, priority 90.
         */
        @Test
        @DisplayName("Spot check: E_012 is Era III SHAMANIC_RITUAL final, priority 90")
        void spotCheckE012() {
            EventCard card = registry.getEvent("E_012");
            assertEquals(Era.III, card.getEra());
            assertEquals(EventType.SHAMANIC_RITUAL, card.getType());
            assertEquals(90, card.getPriority());
            assertTrue(card.isFinal());
        }

        /**
         * Priority of final events must be higher than non-final events.
         * This ensures Sustenance final resolves last.
         */
        @Test
        @DisplayName("Final events have higher priority than non-final events")
        void finalEventsHigherPriority() {
            int maxNonFinal = 0;
            int minFinal = Integer.MAX_VALUE;

            for (int i = 1; i <= 12; i++) {
                String cardID = String.format("E_%03d", i);
                EventCard card = registry.getEvent(cardID);
                if (card.isFinal()) {
                    minFinal = Math.min(minFinal, card.getPriority());
                } else {
                    maxNonFinal = Math.max(maxNonFinal, card.getPriority());
                }
            }
            assertTrue(minFinal > maxNonFinal,
                    "Final event min priority (" + minFinal +
                            ") should exceed non-final max (" + maxNonFinal + ")");
        }

//        /**
//         * Every event card must have a non-null EventEffect.
//         */
//        @Disabled
//        @Test
//        @DisplayName("Every event card has a non-null effect")
//        void allEventsHaveEffect() {
//            for (int i = 1; i <= 12; i++) {
//                String cardID = String.format("E_%03d", i);
//                EventCard card = registry.getEvent(cardID);
//                assertNotNull(card.getEffect(), cardID + " should have a non-null effect"); // we don't have a getEffect()
//            }
//        }

        /**
         * Non-existent event ID should return null.
         */
        @Test
        @DisplayName("Non-existent event ID returns null")
        void nonExistentEventReturnsNull() {
            assertNull(registry.getEvent("E_999"));
        }

        /**
         * Within the same era, events of the same type should not exist
         * (each type appears once per era).
         */
        @Test
        @DisplayName("No duplicate event types within the same era")
        void noDuplicateTypePerEra() {
            Map<Era, Set<EventType>> eraTypes = new EnumMap<>(Era.class);
            for (int i = 1; i <= 12; i++) {
                String cardID = String.format("E_%03d", i);
                EventCard card = registry.getEvent(cardID);
                Set<EventType> types = eraTypes.computeIfAbsent(card.getEra(),
                        k -> EnumSet.noneOf(EventType.class));
                assertTrue(types.add(card.getType()),
                        "Duplicate " + card.getType() + " in " + card.getEra());
            }
        }
    }


    // 3. BUILDING CARDS

    @Nested
    @DisplayName("Building cards loading")
    class BuildingCardTests {

        /**
         * All 21 building cards from the JSON must be loaded (B_001...B_021).
         */
        @Test
        @DisplayName("All 21 building cards are loaded")
        void allBuildingsLoaded() {
            for (int i = 1; i <= 21; i++) {
                String cardID = String.format("B_%03d", i);
                BuildingCard card = registry.getBuilding(cardID);
                assertNotNull(card, "Building card " + cardID + " should be in registry");
            }
        }

        /**
         * Every building card must have valid core fields: non-null ID, era,
         * non-null effectType, buildingCost >= 0, buildingPp >= 0.
         */
        @Test
        @DisplayName("Every building card has valid core fields")
        void buildingFieldsAreValid() {
            for (int i = 1; i <= 21; i++) {
                String cardID = String.format("B_%03d", i);
                BuildingCard card = registry.getBuilding(cardID);

                assertNotNull(card.getCardID(), cardID + " ID should not be null");
                assertNotNull(card.getEra(), cardID + " era should not be null");
                assertNotNull(card.getEffectType(), cardID + " effectType should not be null");
                assertFalse(card.getEffectType().isEmpty(), cardID + " effectType should not be empty");
                assertTrue(card.getBuildingCost() >= 0, cardID + " cost should be >= 0");
                assertTrue(card.getBuildingPp() >= 0, cardID + " PP should be >= 0");
            }
        }

        /**
         * Buildings with effectParams in the JSON should have non-null params.
         * Buildings without effectParams (B_001, B_019) can have null or MissingNode.
         */
        @Test
        @DisplayName("Buildings with effectParams have non-null/non-missing params")
        void buildingsWithParamsHaveParams() {
            // These buildings have effectParams in the JSON
            List<String> withParams = List.of(
                    "B_002", "B_003", "B_004", "B_005", "B_006", "B_007", "B_008",
                    "B_009", "B_010", "B_011", "B_012", "B_013", "B_014", "B_015",
                    "B_016", "B_017", "B_018", "B_020", "B_021"
            );

            for (String cardID : withParams) {
                BuildingCard card = registry.getBuilding(cardID);
                assertNotNull(card.getEffectParams(),
                        cardID + " should have non-null effectParams");
                assertFalse(card.getEffectParams().isMissingNode(),
                        cardID + " effectParams should not be MissingNode");
            }
        }

        /**
         * Verifies the distribution of buildings per era.
         * From the JSON: Era I = 6, Era II = 7, Era III = 8.
         */
        @Test
        @DisplayName("Building distribution per era: I=6, II=7, III=8")
        void buildingCountPerEra() {
            Map<Era, Integer> counts = new EnumMap<>(Era.class);
            for (int i = 1; i <= 21; i++) {
                String cardID = String.format("B_%03d", i);
                Era era = registry.getBuilding(cardID).getEra();
                counts.merge(era, 1, Integer::sum);
            }

            // Era I: B_001, B_002, B_003, B_004, B_005, B_019 = 6
            // Era II: B_006, B_007, B_008, B_015, B_016, B_017, B_018 = 7
            // Era III: B_009, B_010, B_011, B_012, B_013, B_014, B_020, B_021 = 8
            assertEquals(6, counts.getOrDefault(Era.I, 0), "Era I buildings");
            assertEquals(7, counts.getOrDefault(Era.II, 0), "Era II buildings");
            assertEquals(8, counts.getOrDefault(Era.III, 0), "Era III buildings");
        }

        /**
         * Spot check: B_001 is Era I, cost 3, PP 3, effectType TURN_ORDER_FOOD_BONUS.
         */
        @Test
        @DisplayName("Spot check: B_001 is Era I, cost 3, PP 3, TURN_ORDER_FOOD_BONUS")
        void spotCheckB001() {
            BuildingCard card = registry.getBuilding("B_001");
            assertEquals(Era.I, card.getEra());
            assertEquals(3, card.getBuildingCost());
            assertEquals(3, card.getBuildingPp());
            assertEquals("TURN_ORDER_FOOD_BONUS", card.getEffectType());
        }

        /**
         * Spot check: B_021 is Era III, cost 10, PP 0, ENDGAME_FLAT_PP with bonusPP=25.
         */
        @Test
        @DisplayName("Spot check: B_021 is Era III, cost 10, PP 0, ENDGAME_FLAT_PP")
        void spotCheckB021() {
            BuildingCard card = registry.getBuilding("B_021");
            assertEquals(Era.III, card.getEra());
            assertEquals(10, card.getBuildingCost());
            assertEquals(0, card.getBuildingPp());
            assertEquals("ENDGAME_FLAT_PP", card.getEffectType());
            assertEquals(25, card.getEffectParams().get("bonusPP").asInt());
        }

        /**
         * Spot check: B_020 is EXTRA_TURN with extraUpperPicks=1, extraLowerPicks=0.
         */
        @Test
        @DisplayName("Spot check: B_020 EXTRA_TURN params")
        void spotCheckB020() {
            BuildingCard card = registry.getBuilding("B_020");
            assertEquals("EXTRA_TURN", card.getEffectType());
            assertEquals(1, card.getEffectParams().get("extraUpperPicks").asInt());
            assertEquals(0, card.getEffectParams().get("extraLowerPicks").asInt());
        }

        /**
         * Spot check: B_004 EVENT_CHARACTER_BONUS params are correctly parsed.
         */
        @Test
        @DisplayName("Spot check: B_004 EVENT_CHARACTER_BONUS params")
        void spotCheckB004() {
            BuildingCard card = registry.getBuilding("B_004");
            assertEquals("EVENT_CHARACTER_BONUS", card.getEffectType());
            assertEquals("SUSTENANCE", card.getEffectParams().get("eventType").asText());
            assertEquals("GATHERER", card.getEffectParams().get("characterType").asText());
            assertEquals(1, card.getEffectParams().get("foodDiscount").asInt());
            assertEquals(0, card.getEffectParams().get("foodReward").asInt());
            assertEquals(0, card.getEffectParams().get("prestigeReward").asInt());
        }

        /**
         * Non-existent building ID should return null.
         */
        @Test
        @DisplayName("Non-existent building ID returns null")
        void nonExistentBuildingReturnsNull() {
            assertNull(registry.getBuilding("B_999"));
        }

        /**
         * Every effectType string in the JSON must correspond to a known case
         * in BuildingFactory. We verify the complete set of effect types.
         */
        @Test
        @DisplayName("All effectType strings are from the known set")
        void allEffectTypesAreKnown() {
            Set<String> knownTypes = Set.of(
                    "TURN_ORDER_FOOD_BONUS", "INVENTOR_PAIR_FOOD_REWARD",
                    "FULL_SET_FOOD_REWARD", "EVENT_CHARACTER_BONUS",
                    "EXTRA_SHAMAN_STARS", "SHAMANIC_IMMUNITY",
                    "SHAMANIC_WIN_MULTIPLIER", "ENDGAME_CHARACTER_PP",
                    "ENDGAME_FULL_SET_PP", "PP_MULTIPLIER_PER_TYPE",
                    "ENDGAME_FLAT_PP", "EXTRA_TURN"
            );

            for (int i = 1; i <= 21; i++) {
                String cardID = String.format("B_%03d", i);
                String effectType = registry.getBuilding(cardID).getEffectType();
                assertTrue(knownTypes.contains(effectType),
                        cardID + " has unknown effectType: " + effectType);
            }
        }
    }


    // 4. OFFER TILES

    @Nested
    @DisplayName("Offer tiles loading")
    class OfferTileTests {

        /**
         * For a 2-player game: tiles B, C, E, F should be available (minPlayers <= 2).
         * That's 4 tiles.
         */
        @Test
        @DisplayName("2-player game: 4 offer tiles available")
        void twoPlayerOfferTiles() {
            List<OfferTile> tiles = registry.getOfferTiles(2);
            assertEquals(4, tiles.size());
        }

        /**
         * For a 3-player game: tiles B, C, D, E, F should be available.
         * That's 5 tiles.
         */
        @Test
        @DisplayName("3-player game: 5 offer tiles available")
        void threePlayerOfferTiles() {
            List<OfferTile> tiles = registry.getOfferTiles(3);
            assertEquals(5, tiles.size());
        }

        /**
         * For a 4-player game: tiles B, C, D, E, F, G should be available.
         * That's 6 tiles.
         */
        @Test
        @DisplayName("4-player game: 6 offer tiles available")
        void fourPlayerOfferTiles() {
            List<OfferTile> tiles = registry.getOfferTiles(4);
            assertEquals(6, tiles.size());
        }

        /**
         * For a 5-player game: all 7 tiles (A...G) should be available.
         */
        @Test
        @DisplayName("5-player game: all 7 offer tiles available")
        void fivePlayerOfferTiles() {
            List<OfferTile> tiles = registry.getOfferTiles(5);
            assertEquals(7, tiles.size());
        }

        /**
         * Every offer tile must have gainedFood >= 0 and consistent pick limits.
         */
        @Test
        @DisplayName("All offer tiles have valid fields")
        void offerTileFieldsValid() {
            List<OfferTile> allTiles = registry.getOfferTiles(5);
            for (OfferTile tile : allTiles) {
                assertTrue(tile.getGainedFood() >= 0,
                        "Tile " + tile.getTileID() + " gainedFood should be >= 0");
                assertTrue(tile.getNumUpperChoosable() >= 0,
                        "Tile " + tile.getTileID() + " numUpperChoosable should be >= 0");
                assertTrue(tile.getNumLowerChoosable() >= 0,
                        "Tile " + tile.getTileID() + " numLowerChoosable should be >= 0");
                assertTrue(tile.getMinPlayers() >= 2,
                        "Tile " + tile.getTileID() + " minPlayers should be >= 2");
            }
        }

        /**
         * Tile A (5 players only) gives 3 food and 0 picks from either row.
         */
        @Test
        @DisplayName("Tile A: 3 food, 0 picks, minPlayers 5")
        void tileADetails() {
            List<OfferTile> tiles = registry.getOfferTiles(5);
            OfferTile tileA = tiles.stream()
                    .filter(t -> t.getTileID() == 'A')
                    .findFirst()
                    .orElseThrow();

            assertEquals(3, tileA.getGainedFood());
            assertEquals(0, tileA.getNumUpperChoosable());
            assertEquals(0, tileA.getNumLowerChoosable());
            assertEquals(5, tileA.getMinPlayers());
        }

        /**
         * Tile E: 0 food, 1 upper pick, 1 lower pick, minPlayers 2.
         */
        @Test
        @DisplayName("Tile E: 0 food, 1 upper + 1 lower, minPlayers 2")
        void tileEDetails() {
            List<OfferTile> tiles = registry.getOfferTiles(2);
            OfferTile tileE = tiles.stream()
                    .filter(t -> t.getTileID() == 'E')
                    .findFirst()
                    .orElseThrow();

            assertEquals(0, tileE.getGainedFood());
            assertEquals(1, tileE.getNumUpperChoosable());
            assertEquals(1, tileE.getNumLowerChoosable());
            assertEquals(2, tileE.getMinPlayers());
        }

        /**
         * Tile G: 0 food, 2 upper, 1 lower, minPlayers 4.
         * Should NOT appear in a 3-player game.
         */
        @Test
        @DisplayName("Tile G is excluded from 3-player games")
        void tileGExcludedForThree() {
            List<OfferTile> tiles = registry.getOfferTiles(3);
            boolean hasG = tiles.stream().anyMatch(t -> t.getTileID() == 'G');
            assertFalse(hasG, "Tile G should not appear in a 3-player game");
        }

        /**
         * No duplicate tile IDs within the same player count.
         */
        @Test
        @DisplayName("No duplicate tile IDs for any player count")
        void noDuplicateTileIDs() {
            for (int n = 2; n <= 5; n++) {
                List<OfferTile> tiles = registry.getOfferTiles(n);
                Set<Character> ids = new HashSet<>();
                for (OfferTile tile : tiles) {
                    assertTrue(ids.add(tile.getTileID()),
                            "Duplicate tile ID '" + tile.getTileID() + "' for " + n + " players");
                }
            }
        }
    }


    // 5. TURN ORDER TILES

    @Nested
    @DisplayName("Turn order tiles loading")
    class TurnOrderTileTests {

        /**
         * TurnOrderTile must exist for 2, 3, 4, 5 players.
         */
        @Test
        @DisplayName("Turn order tiles exist for 2-5 players")
        void turnOrderTilesExistForAllCounts() {
            for (int n = 2; n <= 5; n++) {
                TurnOrderTile tile = registry.getTurnOrderTile(n);
                assertNotNull(tile, "TurnOrderTile for " + n + " players should exist");
            }
        }

//        /**
//         * 2-player turn order tile: foodBonuses = [1, -1], ppMalus = [0, -2].
//         */
//        @Disabled // We don't have a getFoodBonuses or getPrestigePointsMalus
//        @Test
//        @DisplayName("2-player turn order: food [1,-1], malus [0,-2]")
//        void twoPlayerTurnOrder() {
//            TurnOrderTile tile = registry.getTurnOrderTile(2);
//            assertEquals(List.of(1, -1), tile.getFoodBonuses());
//            assertEquals(List.of(0, -2), tile.getPrestigePointsMalus());
//        }
//
//        /**
//         * 5-player turn order tile: foodBonuses = [3,1,0,0,-1], ppMalus = [0,0,0,0,-2].
//         */
//        @Disabled("We don't have a getFoodBonuses or getPrestigePointsMalus")
//        @Test
//        @DisplayName("5-player turn order: food [3,1,0,0,-1], malus [0,0,0,0,-2]")
//        void fivePlayerTurnOrder() {
//            TurnOrderTile tile = registry.getTurnOrderTile(5);
//            assertEquals(List.of(3, 1, 0, 0, -1), tile.getFoodBonuses());
//            assertEquals(List.of(0, 0, 0, 0, -2), tile.getPrestigePointsMalus());
//        }
//
//        /**
//         * The last position always has a -2 PP malus and a food penalty (-1).
//         */
//        @Disabled("We don't have a getFoodBonuses or getPrestigePointsMalus")
//        @Test
//        @DisplayName("Last position always has -2 PP malus")
//        void lastPositionAlwaysMalus() {
//            for (int n = 2; n <= 5; n++) {
//                TurnOrderTile tile = registry.getTurnOrderTile(n);
//                List<Integer> malus = tile.getPrestigePointsMalus();
//                assertEquals(-2, malus.get(malus.size() - 1),
//                        "Last position for " + n + " players should have -2 PP malus");
//            }
//        }
//
//        /**
//         * The first position always has 0 PP malus (no penalty).
//         */
//        @Disabled("We don't have a getFoodBonuses or getPrestigePointsMalus")
//        @Test
//        @DisplayName("First position never has PP malus")
//        void firstPositionNoMalus() {
//            for (int n = 2; n <= 5; n++) {
//                TurnOrderTile tile = registry.getTurnOrderTile(n);
//                assertEquals(0, tile.getPrestigePointsMalus().get(0),
//                        "First position for " + n + " players should have 0 PP malus");
//            }
//        }
//    }


        // 6. CROSS-DOMAIN CONSISTENCY

        @Nested
        @DisplayName("Cross-domain consistency checks")
        class CrossDomainTests {

            /**
             * No card ID should appear in more than one registry map.
             * Character IDs (C_xxx), Event IDs (E_xxx), and Building IDs (B_xxx)
             * must be mutually exclusive.
             */
            @Test
            @DisplayName("No ID collisions between characters, events, and buildings")
            void noIdCollisions() {
                Set<String> allIDs = new HashSet<>();

                for (int i = 1; i <= 84; i++) {
                    String id = String.format("C_%03d", i);
                    assertTrue(allIDs.add(id), "Duplicate ID: " + id);
                }
                for (int i = 1; i <= 12; i++) {
                    String id = String.format("E_%03d", i);
                    assertTrue(allIDs.add(id), "Duplicate ID: " + id);
                }
                for (int i = 1; i <= 21; i++) {
                    String id = String.format("B_%03d", i);
                    assertTrue(allIDs.add(id), "Duplicate ID: " + id);
                }
            }

            /**
             * The registry must correctly distinguish card types via
             * isEvent() and isBuilding() methods.
             */
            @Test
            @DisplayName("isEvent and isBuilding correctly classify cards")
            void isEventAndIsBuildingClassify() {
                // Events should be recognized as events
                for (int i = 1; i <= 12; i++) {
                    String id = String.format("E_%03d", i);
                    assertTrue(registry.isEvent(id), id + " should be recognized as event");
                    assertFalse(registry.isBuilding(id), id + " should not be a building");
                }

                // Buildings should be recognized as buildings
                for (int i = 1; i <= 21; i++) {
                    String id = String.format("B_%03d", i);
                    assertTrue(registry.isBuilding(id), id + " should be recognized as building");
                    assertFalse(registry.isEvent(id), id + " should not be an event");
                }

                // Characters should be neither events nor buildings
                for (int i = 1; i <= 84; i++) {
                    String id = String.format("C_%03d", i);
                    assertFalse(registry.isEvent(id), id + " should not be an event");
                    assertFalse(registry.isBuilding(id), id + " should not be a building");
                }
            }
        }

    }
}