package it.polimi.ingsw.am02.client.view.gui;

import javafx.scene.image.Image;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for loading and caching images used in the GUI.
 * This class ensures that images are only loaded once and can be accessed efficiently.
 */
public class ImageLoader {
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves an image from the cache or loads it from the resources if not present.
     *
     * @param path The resource path of the image.
     * @return The Image instance.
     * @throws NullPointerException if the image resource is not found.
     */
    public static Image getImage(String path) {
        return cache.computeIfAbsent(path, p ->
                new Image(Objects.requireNonNull(ImageLoader.class.getResourceAsStream(p)))
        );
    }


    /**
     * Preloads the rule images in a background thread to improve GUI responsiveness.
     */
    public static void preloadRulesInBackground() {
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 8; i++) {
                String path = "/images/rules/page" + i + ".png";
                getImage(path);
            }
        });
    }
}