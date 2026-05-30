package it.polimi.ingsw.am02.server.controller.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * File-based {@link GameLogger} that writes NDJSON (newline-delimited JSON)
 * to {@code logs/{gameId}.ndjson}.
 *
 * <p>Each log entry is a self-contained JSON object on its own line:
 * <ul>
 *   <li>{@code GAME_INIT}  — one record at the start with seed, nicknames, and totems</li>
 *   <li>{@code COMMAND}    — one record per player action with a sequence number and nickname</li>
 *   <li>{@code GAME_ENDED} — one record at the end; its presence prevents replay on restart</li>
 * </ul>
 *
 * <p>The writer is opened in append mode and flushed after every line,
 * so entries survive a server crash at any point during the game.
 */public class CommandLogger implements GameLogger {

    private static final String LOGS_DIR = "logs";

    private final ObjectMapper mapper;
    private final BufferedWriter writer;
    private int seq;


    /**
     * Opens (or creates) the log file at {@code logs/{gameId}.ndjson} in append mode.
     *
     * @param gameId the game's unique identifier (used as the filename)
     * @throws IOException if the {@code logs/} directory cannot be created or the file cannot be opened
     */    public CommandLogger(String gameId) throws IOException {
        this.mapper = new ObjectMapper();
        this.seq = 0;
        Files.createDirectories(Path.of(LOGS_DIR));
        Path logFile = Path.of(LOGS_DIR, gameId + ".ndjson");
        this.writer = Files.newBufferedWriter(logFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    // Writes "GAME_INIT" record before any other command
    /** {@inheritDoc} */
    @Override
    public void logGameInit(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "GAME_INIT");
        node.put("gameId", gameId);
        node.put("seed", seed);
        node.put("numPlayers", nicknames.size());
        node.set("nicknames", mapper.valueToTree(nicknames));



        // CORREZIONE: Usiamo una LinkedHashMap per garantire un ordine deterministico
        // nel JSON, seguendo l'ordine della lista nicknames passata dal ControllerManager.
        Map<String, String> orderedTotems = new LinkedHashMap<>();
        for (String nick : nicknames) {
            if (chosenTotems.containsKey(nick)) {
                orderedTotems.put(nick, chosenTotems.get(nick).name());
            }
        }
        node.set("totems", mapper.valueToTree(orderedTotems));

        writeLine(node);
    }

    // Writes a COMMAND after a command has been successfully applied to the model
    /** {@inheritDoc} */
    @Override
    public void logCommand(GameCommand cmd, String senderNickname) {
        try {
            ObjectNode node = mapper.createObjectNode();

            node.put("type", "COMMAND");
            node.put("seq", seq++);

            node.put("nickname", senderNickname);

            node.set("cmd", mapper.valueToTree(cmd));

            writeLine(node);

        } catch (Exception e) {
            System.err.println("[CommandLogger] Failed to log command: " + e.getMessage());
        }
    }

    // Writes "GAME_ENDED" record. After this, the log won't be replayed on restart
    /** {@inheritDoc} */
    @Override
    public void logGameEnded() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "GAME_ENDED");
        writeLine(node);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("[CommandLogger] Failed to close writer: " + e.getMessage());
        }
    }


    // Helper methods
    private void writeLine(ObjectNode node) {
        try {
            writer.write(mapper.writeValueAsString(node));
            writer.newLine();
            writer.flush(); // flushes each line for crash resilience
        } catch (IOException e) {
            System.err.println("[CommandLogger] Write failed: " + e.getMessage());
        }
    }
}