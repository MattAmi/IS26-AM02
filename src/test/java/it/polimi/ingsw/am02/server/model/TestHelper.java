package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.GameRegistry;

/**
 * Shared test utility that loads the {@link GameRegistry} singleton once for the
 * whole test suite, so tests relying on game data do not reload it repeatedly.
 */
public final class TestHelper {

    private static boolean registryLoaded = false;

    private TestHelper() {
        // Utility class — no instantiation
    }

    public static synchronized void ensureRegistryLoaded() {
        if (registryLoaded) {
            return;
        }

        GameRegistry.getInstance();
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