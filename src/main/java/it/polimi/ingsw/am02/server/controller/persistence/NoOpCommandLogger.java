package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.messages.commands.GameCommand;

import java.util.List;

// No-Op Logger (Null Object Pattern): disables persistence without breaking code
public class NoOpCommandLogger implements GameLogger {
    @Override public void logGameInit(String gameId, long seed, List<String> nicknames) {}
    @Override public void logCommand(GameCommand cmd) {}
    @Override public void logGameEnded() {}
    @Override public void close() {}
}
