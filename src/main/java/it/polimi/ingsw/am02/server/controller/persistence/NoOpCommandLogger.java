package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;

import java.util.List;
import java.util.Map;

// No-Op Logger (Null Object Pattern): disables persistence without breaking code
public class NoOpCommandLogger implements GameLogger {
    @Override public void logGameInit(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems) {}
    @Override public void logCommand(GameCommand cmd, String senderNickname) {}
    @Override public void logGameEnded() {}
    @Override public void close() {}
}
