package it.polimi.ingsw.am02.client.view.gui;

import javafx.scene.image.Image;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class ImageLoader {
    // ConcurrentHashMap è obbligatoria per leggere e scrivere da thread diversi in sicurezza
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    public static Image getImage(String path) {
        // computeIfAbsent è super efficiente: carica l'immagine solo se non c'è già
        return cache.computeIfAbsent(path, p ->
                new Image(Objects.requireNonNull(ImageLoader.class.getResourceAsStream(p)))
        );
    }


    public static void preloadRulesInBackground() {
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 8; i++) {
                String path = "/it.polimi.ingsw.am02.images/rules/page" + i + ".png";
                getImage(path);
            }
        });
    }
}