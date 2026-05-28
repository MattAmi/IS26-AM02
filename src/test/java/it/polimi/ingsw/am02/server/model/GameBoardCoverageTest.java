package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.GameEventEmitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameBoardCoverageTest {

    private GameBoard gameBoard;
    private GameEventEmitter mockNotifier;
    private GameRegistry registry;
    private Map<String, EventCard> originalEventMap;
    private Map<String, BuildingCard> originalBuildingMap;
    private Map<String, CharacterCard> originalCharacterMap;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        registry = GameRegistry.getInstance();

        // Save original maps
        Field eventMapField = GameRegistry.class.getDeclaredField("eventMap");
        eventMapField.setAccessible(true);
        originalEventMap = new HashMap<>((Map<String, EventCard>) eventMapField.get(registry));

        Field buildingMapField = GameRegistry.class.getDeclaredField("buildingMap");
        buildingMapField.setAccessible(true);
        originalBuildingMap = new HashMap<>((Map<String, BuildingCard>) buildingMapField.get(registry));

        Field characterMapField = GameRegistry.class.getDeclaredField("characterMap");
        characterMapField.setAccessible(true);
        originalCharacterMap = new HashMap<>((Map<String, CharacterCard>) characterMapField.get(registry));

        // Clear maps for isolation
        ((Map<String, EventCard>) eventMapField.get(registry)).clear();
        ((Map<String, BuildingCard>) buildingMapField.get(registry)).clear();
        ((Map<String, CharacterCard>) characterMapField.get(registry)).clear();

        mockNotifier = mock(GameEventEmitter.class);
        gameBoard = new GameBoard(2, mockNotifier, new Random(123));
    }

    @AfterEach
    @SuppressWarnings("unchecked")
    void tearDown() throws Exception {
        Field eventMapField = GameRegistry.class.getDeclaredField("eventMap");
        eventMapField.setAccessible(true);
        Map<String, EventCard> eMap = (Map<String, EventCard>) eventMapField.get(registry);
        eMap.clear();
        eMap.putAll(originalEventMap);

        Field buildingMapField = GameRegistry.class.getDeclaredField("buildingMap");
        buildingMapField.setAccessible(true);
        Map<String, BuildingCard> bMap = (Map<String, BuildingCard>) buildingMapField.get(registry);
        bMap.clear();
        bMap.putAll(originalBuildingMap);

        Field characterMapField = GameRegistry.class.getDeclaredField("characterMap");
        characterMapField.setAccessible(true);
        Map<String, CharacterCard> cMap = (Map<String, CharacterCard>) characterMapField.get(registry);
        cMap.clear();
        cMap.putAll(originalCharacterMap);
    }

    private void addMockEvent(String id, boolean isFinal, int priority) throws Exception {
        EventCard mockEvent = mock(EventCard.class);
        when(mockEvent.getID()).thenReturn(id);
        when(mockEvent.isFinal()).thenReturn(isFinal);
        when(mockEvent.getPriority()).thenReturn(priority);
        when(mockEvent.getType()).thenReturn(EventType.SHAMANIC_RITUAL); // arbitrary
        
        Field eventMapField = GameRegistry.class.getDeclaredField("eventMap");
        eventMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, EventCard> eventMap = (Map<String, EventCard>) eventMapField.get(registry);
        eventMap.put(id, mockEvent);
    }

    @Test
    void testResolveRoundEvents() throws Exception {
        addMockEvent("E_ROUND_1", false, 1);
        addMockEvent("E_ROUND_2", false, 2);
        addMockEvent("E_FINAL_1", true, 3); // should be ignored

        Field lowerRowField = GameBoard.class.getDeclaredField("lowerRow");
        lowerRowField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> lowerRow = (List<String>) lowerRowField.get(gameBoard);
        lowerRow.clear();
        lowerRow.add("E_ROUND_2");
        lowerRow.add("E_ROUND_1");
        lowerRow.add("E_FINAL_1");

        EventObserver mockObserver = mock(EventObserver.class);
        gameBoard.attachEventObserver(mockObserver);

        List<Player> players = new ArrayList<>();
        gameBoard.resolveRoundEvents(players);

        EventCard e1 = registry.getEvent("E_ROUND_1");
        EventCard e2 = registry.getEvent("E_ROUND_2");
        
        verify(e1, times(1)).applyEventEffect(eq(players), anyList());
        verify(e2, times(1)).applyEventEffect(eq(players), anyList());
        verify(mockObserver, times(2)).eventStart(any());
        verify(mockObserver, times(2)).eventEnd(any());
        verify(mockNotifier, times(2)).notifyEventResolved(anyString(), anyString());
    }

    @Test
    void testResolveFinalEvents() throws Exception {
        addMockEvent("E_FINAL_1", true, 1);
        addMockEvent("E_FINAL_2", true, 2);
        addMockEvent("E_ROUND_1", false, 3); // should be ignored

        Field upperRowField = GameBoard.class.getDeclaredField("upperRow");
        upperRowField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> upperRow = (List<String>) upperRowField.get(gameBoard);
        upperRow.clear();
        upperRow.add("E_FINAL_1");

        Field lowerRowField = GameBoard.class.getDeclaredField("lowerRow");
        lowerRowField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> lowerRow = (List<String>) lowerRowField.get(gameBoard);
        lowerRow.clear();
        lowerRow.add("E_FINAL_2");
        lowerRow.add("E_ROUND_1");

        EventObserver mockObserver = mock(EventObserver.class);
        gameBoard.attachEventObserver(mockObserver);

        List<Player> players = new ArrayList<>();
        gameBoard.resolveFinalEvents(players);

        EventCard e1 = registry.getEvent("E_FINAL_1");
        EventCard e2 = registry.getEvent("E_FINAL_2");

        verify(e1, times(1)).applyEventEffect(eq(players), anyList());
        verify(e2, times(1)).applyEventEffect(eq(players), anyList());
        verify(mockObserver, times(2)).eventStart(any());
        verify(mockObserver, times(2)).eventEnd(any());
    }

    @Test
    void testUpdateRowsForNewEra() throws Exception {
        Field currentEraField = GameBoard.class.getDeclaredField("currentEra");
        currentEraField.setAccessible(true);
        currentEraField.set(gameBoard, Era.II);

        Field upperRowBField = GameBoard.class.getDeclaredField("upperRowBuildings");
        upperRowBField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> upperRowB = (List<String>) upperRowBField.get(gameBoard);
        upperRowB.clear();
        upperRowB.add("B_1");

        Field lowerRowBField = GameBoard.class.getDeclaredField("lowerRowBuildings");
        lowerRowBField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> lowerRowB = (List<String>) lowerRowBField.get(gameBoard);
        lowerRowB.clear();
        lowerRowB.add("B_OLD");

        BuildingDeck mockDeck = mock(BuildingDeck.class);
        when(mockDeck.getBuildingsForEra(Era.II)).thenReturn(List.of("B_2", "B_3"));
        Field deckField = GameBoard.class.getDeclaredField("buildingDeck");
        deckField.setAccessible(true);
        deckField.set(gameBoard, mockDeck);

        gameBoard.updateRowsForNewEra();

        assertEquals(List.of("B_2", "B_3"), upperRowB);
        assertEquals(List.of("B_OLD", "B_1"), lowerRowB);
        verify(mockNotifier, times(1)).notifyEraChanged(eq(Era.II), anyList(), anyList(), anyList());

        // Test for Era III
        currentEraField.set(gameBoard, Era.III);
        when(mockDeck.getBuildingsForEra(Era.III)).thenReturn(List.of("B_4"));
        
        gameBoard.updateRowsForNewEra();
        assertEquals(List.of("B_4"), upperRowB);
        assertEquals(List.of("B_2", "B_3"), lowerRowB); // B_OLD and B_1 are discarded
    }

    @Test
    void testPrepareNewRound() throws Exception {
        Field upperRowField = GameBoard.class.getDeclaredField("upperRow");
        upperRowField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> upperRow = (List<String>) upperRowField.get(gameBoard);
        upperRow.clear();
        upperRow.add("U1");
        upperRow.add("U2");

        Field lowerRowField = GameBoard.class.getDeclaredField("lowerRow");
        lowerRowField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> lowerRow = (List<String>) lowerRowField.get(gameBoard);
        lowerRow.clear();
        lowerRow.add("L1");

        TribuDeck mockDeck = mock(TribuDeck.class);
        when(mockDeck.isEmpty()).thenReturn(false, false, true); // draw 2 cards then empty
        when(mockDeck.draw()).thenReturn("N1", "N2");
        when(mockDeck.getRemainingSize()).thenReturn(0);

        Field deckField = GameBoard.class.getDeclaredField("tribuDeck");
        deckField.setAccessible(true);
        deckField.set(gameBoard, mockDeck);

        CharacterCard mockChar1 = mock(CharacterCard.class);
        when(mockChar1.getEra()).thenReturn(Era.II);
        Field charMapField = GameRegistry.class.getDeclaredField("characterMap");
        charMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, CharacterCard> charMap = (Map<String, CharacterCard>) charMapField.get(registry);
        charMap.put("N1", mockChar1);
        charMap.put("N2", mockChar1);

        Field currentEraField = GameBoard.class.getDeclaredField("currentEra");
        currentEraField.setAccessible(true);
        currentEraField.set(gameBoard, Era.I);

        gameBoard.prepareNewRound(2);

        assertTrue(gameBoard.hasEraChanged());
        assertEquals(List.of("N1", "N2"), upperRow);
        assertEquals(List.of("U1", "U2"), lowerRow);
        verify(mockNotifier, times(1)).notifyBoardUpdated(anyList(), anyList(), anyList(), anyList(), anyInt());
    }
}
