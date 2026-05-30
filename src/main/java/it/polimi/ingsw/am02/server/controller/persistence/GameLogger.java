package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;
import java.util.List;
import java.util.Map;

/**
 * Strategy interface for persisting the sequence of game commands
 * to a durable log. Enables crash recovery via NDJSON replay.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link CommandLogger} — writes to a file under {@code logs/}</li>
 *   <li>{@link NoOpCommandLogger} — discards all writes (Null Object Pattern)</li>
 * </ul>
 */
public interface GameLogger {

    /**
     * Writes the {@code GAME_INIT} record. Must be called once before
     * any {@link #logCommand} calls.
     *
     * @param gameId      the game's unique identifier
     * @param seed        the random seed used to initialize the game
     * @param nicknames   ordered list of player nicknames
     * @param chosenTotems mapping from nickname to chosen totem
     */
    void logGameInit(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems);


    /**
     * Writes a {@code COMMAND} record after the command has been
     * successfully applied to the model.
     *
     * @param cmd            the command that was executed
     * @param senderNickname the player who issued the command
     */
    void logCommand(GameCommand cmd, String senderNickname);

    /**
     * Writes the {@code GAME_ENDED} record. After this entry the log
     * will not be replayed on server restart.
     */
    void logGameEnded();

    /**
     * Flushes and closes the underlying writer, releasing any file handles.
     * Must be called when the game session ends.
     */
    void close();
}