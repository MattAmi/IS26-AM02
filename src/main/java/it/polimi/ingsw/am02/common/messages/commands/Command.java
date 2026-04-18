package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.messages.Message;

/**
 * Marker for all client → server messages. Every concrete command corresponds
 * to exactly one method of {@code VirtualServer}.
 *
 * <p>Commands are immutable records; once built they are only read, never mutated.
 */
public sealed interface Command extends Message
        permits SetUsernameCommand, CreateLobbyCommand, JoinLobbyCommand,
        SelectTotemCommand, StartGameCommand, LeaveLobbyCommand,
        MoveTotemCommand, ResolveActionsCommand {
}