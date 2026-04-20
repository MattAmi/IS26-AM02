package it.polimi.ingsw.am02.common.messages.commands;

public record JoinLobbyCommand(String lobbyID) implements LobbyCommand {}