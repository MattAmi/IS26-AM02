package it.polimi.ingsw.am02.server.controller.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Log-class that enables replay of a game after a server crash
public class CommandLogger implements GameLogger {

    private static final String LOGS_DIR = "logs";

    private final ObjectMapper mapper;
    private final BufferedWriter writer;
    private int seq;


    // Opens (or creates) the log file at logs/{gameId}.ndjson in append mode.
    public CommandLogger(String gameId) throws IOException {
        this.mapper = new ObjectMapper();
        this.seq = 0;
        Files.createDirectories(Path.of(LOGS_DIR));
        Path logFile = Path.of(LOGS_DIR, gameId + ".ndjson");
        this.writer = Files.newBufferedWriter(logFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    // Writes "GAME_INIT" record before any other command
    @Override
    public void logGameInit(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "GAME_INIT");
        node.put("gameId", gameId);
        node.put("seed", seed);
        node.put("numPlayers", nicknames.size());
        node.set("nicknames", mapper.valueToTree(nicknames));
        node.set("totems", mapper.valueToTree(
                chosenTotems.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name()))
        ));
        writeLine(node);
    }

    // Writes a COMMAND after a command has been successfully applied to the model
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
    @Override
    public void logGameEnded() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "GAME_ENDED");
        writeLine(node);
    }

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