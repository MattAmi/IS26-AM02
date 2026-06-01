package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.server.model.card.CharacterCard;
import it.polimi.ingsw.am02.server.model.card.EventCard;
import it.polimi.ingsw.am02.server.model.card.TribuDeck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class TribuDeckTest {

    private final Random gameRandom = new Random(42);
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    @ParameterizedTest(name = "Test TribuDeck with {0} players")
    @CsvSource({
            "2, 63",
            "3, 74",
            "4, 85",
            "5, 96"
    })
    public void testTribuDeck_WithJSONLogic(int numPlayers, int expectedSize) {
        GameRegistry mockRegistry = Mockito.mock(GameRegistry.class);

        try (MockedStatic<GameRegistry> mockedStatic = Mockito.mockStatic(GameRegistry.class)) {
            mockedStatic.when(GameRegistry::getInstance).thenReturn(mockRegistry);

            setupMockTribuData(mockRegistry);

            TribuDeck deck = new TribuDeck(numPlayers, gameRandom);

            assertEquals(expectedSize, deck.getRemainingSize(), "Player filter error: expected " + expectedSize + " cards");

            // Collect drawn cards to verify ordering and total amount
            List<String> drawnCards = verifyStratificationAndDraw(deck, mockRegistry);

            assertEquals(expectedSize, drawnCards.size(), "Drawn cards count does not match the expected size");

            printSuccess(numPlayers, expectedSize);
        }
    }

    // --- STRATIFICATION AND ORDERING VERIFICATION ---
    private List<String> verifyStratificationAndDraw(TribuDeck deck, GameRegistry registry) {
        List<String> drawnCards = new ArrayList<>();
        int expectedLogicalStep = 1;

        while (!deck.isEmpty()) {
            String drawnCardId = deck.draw();
            drawnCards.add(drawnCardId);

            int currentCardStep = getStepFromCardObject(drawnCardId, registry);

            assertTrue(currentCardStep >= expectedLogicalStep,
                    "Stratification violation: Drawn card " + drawnCardId + " (Step " + currentCardStep + ") but expected to be at Step " + expectedLogicalStep);

            expectedLogicalStep = currentCardStep;
        }
        return drawnCards;
    }

    private int getStepFromCardObject(String id, GameRegistry registry) {
        // 1. Check if it's an Event via Registry
        EventCard event = registry.getEvent(id);
        if (event != null) {
            if (event.isFinal()) return 4; // Final event overrides the Era
            return switch (event.getEra()) {
                case I -> 1;
                case II -> 2;
                case III -> 3;
                default -> 0;
            };
        }

        // 2. If not an Event, check if it's a Character
        CharacterCard character = registry.getCharacter(id);
        if (character != null) {
            return switch (character.getEra()) {
                case I -> 1;
                case II -> 2;
                case III -> 3;
                default -> 0;
            };
        }

        // 3. Safety fallback for missing IDs
        fail("The drawn card with ID " + id + " does not exist in the GameRegistry!");
        return 0;
    }

    private void printSuccess(int numPlayers, int expectedSize) {
        System.out.println(ANSI_GREEN + "✅ TribuDeck setup for " + numPlayers + " players PASSED!" + ANSI_RESET);
        System.out.println("   Cards filtered and stacked (LIFO): " + expectedSize);
        System.out.println("---------------------------------------------------");
    }

    // --- MOCK SETUP HELPER (JSON LOGIC) ---
    private void setupMockTribuData(GameRegistry registry) {
        List<String> charIds = new ArrayList<>();
        int cId = 1;

        // Era I (29 Characters)
        for(int i=0; i<17; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.I, 2);
        for(int i=0; i<4; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.I, 3);
        for(int i=0; i<5; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.I, 4);
        for(int i=0; i<3; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.I, 5);

        // Era II (28 Characters)
        for(int i=0; i<17; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.II, 2);
        for(int i=0; i<4; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.II, 3);
        for(int i=0; i<3; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.II, 4);
        for(int i=0; i<4; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.II, 5);

        // Era III (27 Characters)
        for(int i=0; i<17; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.III, 2);
        for(int i=0; i<3; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.III, 3);
        for(int i=0; i<3; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.III, 4);
        for(int i=0; i<4; i++) addMockCharacter(registry, charIds, "C_" + (cId++), Era.III, 5);

        when(registry.getAllCharactersIDs()).thenReturn(charIds);

        // 12 Events (2 Final)
        List<String> eventIds = new ArrayList<>();
        int eId = 1;
        for(int i=0; i<4; i++) addMockEvent(registry, eventIds, "E_" + (eId++), Era.I, false);
        for(int i=0; i<4; i++) addMockEvent(registry, eventIds, "E_" + (eId++), Era.II, false);
        for(int i=0; i<2; i++) addMockEvent(registry, eventIds, "E_" + (eId++), Era.III, false);
        for(int i=0; i<2; i++) addMockEvent(registry, eventIds, "E_" + (eId++), Era.III, true);

        when(registry.getAllEventsIDs()).thenReturn(eventIds);
    }

    private void addMockCharacter(GameRegistry registry, List<String> ids, String id, Era era, int minPlayers) {
        ids.add(id);
        CharacterCard card = Mockito.mock(CharacterCard.class);
        when(card.getEra()).thenReturn(era);
        when(card.getMinPlayers()).thenReturn(minPlayers);
        when(registry.getCharacter(id)).thenReturn(card);
    }

    private void addMockEvent(GameRegistry registry, List<String> ids, String id, Era era, boolean isFinal) {
        ids.add(id);
        EventCard card = Mockito.mock(EventCard.class);
        when(card.getEra()).thenReturn(era);
        when(card.isFinal()).thenReturn(isFinal);
        when(registry.getEvent(id)).thenReturn(card);
    }
}