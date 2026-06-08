package it.polimi.ingsw.am02.common.messages.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Marker interface for commands sent during an active game.
 * All game commands carry the sender's {@link #nickname()} so that the
 * server can route them to the correct game session without relying
 * solely on the client ID.
 */
public sealed interface GameCommand extends Command
        permits MoveTotemCommand, ResolveActionsCommand {
    String nickname();
}
