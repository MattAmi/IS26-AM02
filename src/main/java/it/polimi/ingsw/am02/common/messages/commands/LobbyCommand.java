package it.polimi.ingsw.am02.common.messages.commands;

public sealed interface LobbyCommand extends Command
        permits CreateLobbyCommand, JoinLobbyCommand, LeaveLobbyCommand, PongCommand, SelectTotemCommand, SetUsernameCommand, StartGameCommand {}
