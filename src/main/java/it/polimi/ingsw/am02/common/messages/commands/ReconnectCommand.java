package it.polimi.ingsw.am02.common.messages.commands;

public record ReconnectCommand(String nickname, String gameId) implements Command {}
