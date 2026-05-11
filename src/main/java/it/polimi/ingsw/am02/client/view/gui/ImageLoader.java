package it.polimi.ingsw.am02.client.view.gui;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ImageLoader {
    private static final Map<String, Image> cache = new HashMap<>();

    public static Image getImage(String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        Image img = new Image(Objects.requireNonNull(ImageLoader.class.getResourceAsStream(path)));
        cache.put(path, img);
        return img;

    }
}