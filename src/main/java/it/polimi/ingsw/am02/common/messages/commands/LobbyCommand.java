package it.polimi.ingsw.am02.common.messages.commands;

public sealed interface LobbyCommand extends Command
        permits SetUsernameCommand, CreateLobbyCommand, JoinLobbyCommand,
        SelectTotemCommand, StartGameCommand, LeaveLobbyCommand {}
