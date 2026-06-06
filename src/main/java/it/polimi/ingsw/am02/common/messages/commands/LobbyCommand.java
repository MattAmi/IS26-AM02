package it.polimi.ingsw.am02.common.messages.commands;

/**
 * Marker interface for commands sent during the pre-game (lobby) phase.
 * Covers username selection, lobby creation/joining/leaving, totem selection,
 * and game start acknowledgement.
 *
 * <p>Heartbeat commands ({@code PingCommand}, {@code PongCommand}) are no longer
 * part of this interface — they live under
 * {@link it.polimi.ingsw.am02.common.messages.commands.heartbeat.HeartbeatCommand}.
 */
public sealed interface LobbyCommand extends Command
        permits CreateLobbyCommand, JoinLobbyCommand, LeaveLobbyCommand,
        SelectTotemCommand, SetUsernameCommand, StartGameCommand {}