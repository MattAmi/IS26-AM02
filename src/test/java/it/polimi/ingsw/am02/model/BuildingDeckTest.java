package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.Era;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class BuildingDeckTest {

    // Console color codes for output
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    @Test
    public void testBuildingDeck_SetupFor2Players() {
        GameRegistry mockRegistry = Mockito.mock(GameRegistry.class);
        try (MockedStatic<GameRegistry> mockedStatic = Mockito.mockStatic(GameRegistry.class)) {
            mockedStatic.when(GameRegistry::getInstance).thenReturn(mockRegistry);

            List<String> realIds = setupRealRegistryData(mockRegistry);
            when(mockRegistry.getAllBuildingsIDs()).thenReturn(realIds);

            BuildingDeck deck = new BuildingDeck(2);

            assertEquals(1, deck.getBuildingsForEra(Era.I).size(), "Error Era I (2 Players Match). Expected: 1");
            assertEquals(2, deck.getBuildingsForEra(Era.II).size(), "Error Era II (2 Players Match). Expected: 2");
            assertEquals(3, deck.getBuildingsForEra(Era.III).size(), "Error Era III (2 Players Match). Expected: 3");

            // Print success and deck
            printGeneratedDeck("testBuildingDeck_SetupFor2Players", deck);
        }
    }

    @Test
    public void testBuildingDeck_SetupFor3Players() {
        GameRegistry mockRegistry = Mockito.mock(GameRegistry.class);
        try (MockedStatic<GameRegistry> mockedStatic = Mockito.mockStatic(GameRegistry.class)) {
            mockedStatic.when(GameRegistry::getInstance).thenReturn(mockRegistry);

            List<String> realIds = setupRealRegistryData(mockRegistry);
            when(mockRegistry.getAllBuildingsIDs()).thenReturn(realIds);

            BuildingDeck deck = new BuildingDeck(3);

            assertEquals(2, deck.getBuildingsForEra(Era.I).size(), "Error Era I (3 Players Match). Expected: 2");
            assertEquals(2, deck.getBuildingsForEra(Era.II).size(), "Error Era II (3 Players Match). Expected: 2");
            assertEquals(4, deck.getBuildingsForEra(Era.III).size(), "Error Era III (3 Players Match). Expected: 4");

            printGeneratedDeck("testBuildingDeck_SetupFor3Players", deck);
        }
    }

    @Test
    public void testBuildingDeck_SetupFor4Players() {
        GameRegistry mockRegistry = Mockito.mock(GameRegistry.class);
        try (MockedStatic<GameRegistry> mockedStatic = Mockito.mockStatic(GameRegistry.class)) {
            mockedStatic.when(GameRegistry::getInstance).thenReturn(mockRegistry);

            List<String> realIds = setupRealRegistryData(mockRegistry);
            when(mockRegistry.getAllBuildingsIDs()).thenReturn(realIds);

            BuildingDeck deck = new BuildingDeck(4);

            assertEquals(2, deck.getBuildingsForEra(Era.I).size(), "Error Era I (4 Players Match). Expected: 2");
            assertEquals(3, deck.getBuildingsForEra(Era.II).size(), "Error Era II (4 Players Match). Expected: 3");
            assertEquals(4, deck.getBuildingsForEra(Era.III).size(), "Error Era III (4 Players Match). Expected: 4");

            printGeneratedDeck("testBuildingDeck_SetupFor4Players", deck);
        }
    }

    @Test
    public void testBuildingDeck_SetupFor5Players() {
        GameRegistry mockRegistry = Mockito.mock(GameRegistry.class);
        try (MockedStatic<GameRegistry> mockedStatic = Mockito.mockStatic(GameRegistry.class)) {
            mockedStatic.when(GameRegistry::getInstance).thenReturn(mockRegistry);

            List<String> realIds = setupRealRegistryData(mockRegistry);
            when(mockRegistry.getAllBuildingsIDs()).thenReturn(realIds);

            BuildingDeck deck = new BuildingDeck(5);

            assertEquals(2, deck.getBuildingsForEra(Era.I).size(), "Error Era I (5 Players Match). Expected: 3");
            assertEquals(3, deck.getBuildingsForEra(Era.II).size(), "Error Era II (5 Players Match). Expected: 2");
            assertEquals(5, deck.getBuildingsForEra(Era.III).size(), "Error Era III (5 Players Match). Expected: 5");

            printGeneratedDeck("testBuildingDeck_SetupFor5Players", deck);
        }
    }

    // --- HELPER METHODS ---

    /**
     * Helper method to print success in green and show the drawn cards.
     */
    private void printGeneratedDeck(String testName, BuildingDeck deck) {
        System.out.println(ANSI_GREEN + testName + " (BuildingDeck) PASSED!" + ANSI_RESET);
        System.out.println("   Generated deck (shuffled and cut):");
        System.out.println("   - Era I:   " + deck.getBuildingsForEra(Era.I));
        System.out.println("   - Era II:  " + deck.getBuildingsForEra(Era.II));
        System.out.println("   - Era III: " + deck.getBuildingsForEra(Era.III));
        System.out.println("---------------------------------------------------");
    }

    private List<String> setupRealRegistryData(GameRegistry mockRegistry) {
        List<String> allIds = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            addCardToMock(mockRegistry, allIds, String.format("B_%03d", i), Era.I);
        }
        for (int i = 7; i <= 13; i++) {
            addCardToMock(mockRegistry, allIds, String.format("B_%03d", i), Era.II);
        }
        for (int i = 14; i <= 21; i++) {
            addCardToMock(mockRegistry, allIds, String.format("B_%03d", i), Era.III);
        }

        return allIds;
    }

    private void addCardToMock(GameRegistry mockRegistry, List<String> ids, String id, Era era) {
        ids.add(id);
        BuildingCard mockCard = Mockito.mock(BuildingCard.class);
        when(mockCard.getEra()).thenReturn(era);

        when(mockRegistry.getBuilding(id)).thenReturn(mockCard);
    }
}