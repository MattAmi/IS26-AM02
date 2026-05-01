package it.polimi.ingsw.am02.common.messages.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


public sealed interface GameCommand extends Command
        permits MoveTotemCommand, ResolveActionsCommand {
    String nickname();
}
