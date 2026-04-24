package it.polimi.ingsw.am02.common.messages.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MoveTotemCommand.class, name = "MoveTotemCommand"),
        @JsonSubTypes.Type(value = ResolveActionsCommand.class, name = "ResolveActionsCommand"),
})
public sealed interface GameCommand extends Command
        permits MoveTotemCommand, ResolveActionsCommand {
    String nickname();
}
