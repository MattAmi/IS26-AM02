package it.polimi.ingsw.am02.common.messages.commands;

public record MoveTotemCommand(String nickname, char tileID) implements GameCommand {}