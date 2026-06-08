package it.polimi.ingsw.am02.common.messages.commands;

/**
 * Marker interface for commands sent during the pre-game (lobby) phase.
 * Covers username selection, lobby creation/joining/leaving and totem selection.
 */
public sealed interface LobbyCommand extends Command
        permits CreateLobbyCommand, JoinLobbyCommand, LeaveLobbyCommand,
        SelectTotemCommand, SetUsernameCommand {}