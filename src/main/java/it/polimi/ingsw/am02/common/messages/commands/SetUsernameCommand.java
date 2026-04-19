package it.polimi.ingsw.am02.common.messages.commands;

public record SetUsernameCommand(String username) implements Command {}