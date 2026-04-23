package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.messages.commands.GameCommand;
import java.util.List;

public interface GameLogger {
    void logGameInit(String gameId, long seed, List<String> nicknames);
    void logCommand(GameCommand cmd);
    void logGameEnded();
    void close();
}