package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.GameRegistry;

public final class TestHelper {

    private static boolean registryLoaded = false;

    private TestHelper() {
        // Utility class — no instantiation
    }

    public static synchronized void ensureRegistryLoaded() {
        if (registryLoaded) {
            return;
        }

        GameRegistry registry = GameRegistry.getInstance();

        String basePath = resolveResourceDir("it/polimi/ingsw/am02/JSON/");

        registry.loadCharacters(basePath + "Characters.json");
        registry.loadEvents(basePath + "Events.JSON");
        registry.loadBuildings(basePath + "Buildings.JSON");
        registry.loadOfferTiles(basePath + "OfferTiles.JSON");
        registry.loadTurnOrderTiles(basePath + "TurnOrderTiles.JSON");

        registryLoaded = true;
    }

    private static String resolveResourceDir(String resourceDir) {
        java.net.URL url = TestHelper.class.getClassLoader().getResource(resourceDir);
        if (url == null) {
            throw new IllegalStateException(
                    "Resource directory not found on classpath: " + resourceDir
            );
        }

        return url.getPath();
    }
}