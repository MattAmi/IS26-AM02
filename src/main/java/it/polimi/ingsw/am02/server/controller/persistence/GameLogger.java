package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;
import java.util.List;
import java.util.Map;

public interface GameLogger {
    void logGameInit(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems);
    void logCommand(GameCommand cmd);
    void logGameEnded();
    void close();
}