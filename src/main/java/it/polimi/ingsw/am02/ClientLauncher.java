package it.polimi.ingsw.am02;

/**
 * Launcher entry point for the fat JAR.
 * Must NOT extend Application to allow JavaFX to initialize correctly
 * when launched from an unnamed module (fat JAR without JPMS).
 */
public class ClientLauncher {
    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(
                System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        System.setErr(new java.io.PrintStream(
                System.err, true, java.nio.charset.StandardCharsets.UTF_8));
        ClientApp.main(args);
    }
}