package it.polimi.ingsw.am02.server.controller.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads a {@code .ndjson} log file written by {@link CommandLogger}
 * and returns its contents as typed Java objects for replay.
 *
 * <p>The file format is:
 * <ol>
 *   <li>Line 1: {@code GAME_INIT} record</li>
 *   <li>Lines 2…n-1: {@code COMMAND} records (may be interleaved with other types)</li>
 *   <li>Line n: {@code GAME_ENDED} record (absent if the server crashed)</li>
 * </ol>
 */
public final class CommandLogReader {

    private final Path logFile;
    private final ObjectMapper mapper;

    /**
     * @param logFile the path to the {@code .ndjson} log file to read
     */
    public CommandLogReader(Path logFile) {
        this.logFile = logFile;
        this.mapper = new ObjectMapper();
    }

    /**
     * Parses the first line of the log file and returns the game init data.
     *
     * @return the parsed {@link GameInitRecord}
     * @throws IOException if the file is empty or the first line is not GAME_INIT
     */
    public GameInitRecord readGameInit() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.isBlank()) {
                throw new IOException("Log file is empty: " + logFile);
            }
            JsonNode root = mapper.readTree(firstLine);
            if (!"GAME_INIT".equals(root.path("type").asText())) {
                throw new IOException("First line is not GAME_INIT: " + logFile);
            }

            String gameId = root.path("gameId").asText();
            long seed = root.path("seed").asLong();

            List<String> nicknames = new ArrayList<>();
            for (JsonNode n : root.path("nicknames")) {
                nicknames.add(n.asText());
            }

            Map<String, Totem> totems = new LinkedHashMap<>();
            root.path("totems").properties().forEach(entry ->
                    totems.put(entry.getKey(), Totem.valueOf(entry.getValue().asText())));

            return new GameInitRecord(gameId, seed, nicknames, totems);
        }
    }

    /**
     * Reads all COMMAND lines from the log, skipping GAME_INIT and GAME_ENDED.
     *
     * @return the ordered list of {@link CommandRecord} to replay
     * @throws IOException if the file cannot be read
     */
    public List<CommandRecord> readCommands() throws IOException {
        List<CommandRecord> commands = new ArrayList<>();

        try (Stream<String> lines = Files.lines(logFile)) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isBlank()) continue;
                JsonNode root = mapper.readTree(line);
                if (!"COMMAND".equals(root.path("type").asText())) continue;

                // Estrae il comando (che avrà il nickname vuoto)
                GameCommand cmd = mapper.treeToValue(root.path("cmd"), GameCommand.class);

                // Estrae il VERO nickname dalla busta (es. "Matteo")
                String nickname = root.path("nickname").asText();

                // Salva entrambi nel record
                commands.add(new CommandRecord(cmd, nickname));
            }
        }

        return commands;
    }
}
