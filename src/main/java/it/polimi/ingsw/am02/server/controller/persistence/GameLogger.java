package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;
import java.util.List;
import java.util.Map;

public interface GameLogger {
    void logGameInit(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems);
    // Writes a COMMAND after a command has been successfully applied to the model
    void logCommand(GameCommand cmd, String senderNickname);

    void logGameEnded();
    void close();
}