package it.polimi.ingsw.am02.common.messages.commands;

/**
 * Marker interface for commands sent during the pre-game (lobby) phase.
 * Includes username selection, lobby creation/joining/leaving, totem selection,
 * heartbeat response ({@link PongCommand}), and game start acknowledgement.
 */
public sealed interface LobbyCommand extends Command
        permits CreateLobbyCommand, JoinLobbyCommand, LeaveLobbyCommand, PongCommand, SelectTotemCommand, SetUsernameCommand, StartGameCommand {}
