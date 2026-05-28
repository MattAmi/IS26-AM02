package it.polimi.ingsw.am02.server.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRegistryTest {

    @Test
    void testGameRegistryLoaded() {
        GameRegistry registry = GameRegistry.getInstance();

        assertNotNull(registry);

        List<String> characterIDs = registry.getAllCharactersIDs();
        assertFalse(characterIDs.isEmpty());

        List<String> eventIDs = registry.getAllEventsIDs();
        assertFalse(eventIDs.isEmpty());

        List<String> buildingIDs = registry.getAllBuildingsIDs();
        assertFalse(buildingIDs.isEmpty());

        String firstCharId = characterIDs.get(0);
        assertTrue(registry.isCharacter(firstCharId));
        assertNotNull(registry.getCharacter(firstCharId));

        String firstEventId = eventIDs.get(0);
        assertTrue(registry.isEvent(firstEventId));
        assertNotNull(registry.getEvent(firstEventId));

        String firstBuildingId = buildingIDs.get(0);
        assertTrue(registry.isBuilding(firstBuildingId));
        assertNotNull(registry.getBuilding(firstBuildingId));

        assertFalse(registry.isCharacter("NOT_A_CARD"));
        assertFalse(registry.isEvent("NOT_A_CARD"));
        assertFalse(registry.isBuilding("NOT_A_CARD"));

        assertNull(registry.getCharacter("NOT_A_CARD"));
        assertNull(registry.getEvent("NOT_A_CARD"));
        assertNull(registry.getBuilding("NOT_A_CARD"));

        List<OfferTile> offerTiles = registry.getOfferTiles(4);
        assertFalse(offerTiles.isEmpty());

        TurnOrderTile turnOrderTile = registry.getTurnOrderTile(4);
        assertNotNull(turnOrderTile);
        assertEquals(4, turnOrderTile.getNumPlayers());
        
        assertNull(registry.getTurnOrderTile(99)); // invalid num players
    }
}
