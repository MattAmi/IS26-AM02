package it.polimi.ingsw.am02.common.messages.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.am02.common.messages.Message;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SetUsernameCommand.class,    name = "SetUsername"),
        @JsonSubTypes.Type(value = CreateLobbyCommand.class,   name = "CreateLobby"),
        @JsonSubTypes.Type(value = JoinLobbyCommand.class,     name = "JoinLobby"),
        @JsonSubTypes.Type(value = SelectTotemCommand.class,   name = "SelectTotem"),
        @JsonSubTypes.Type(value = StartGameCommand.class,     name = "StartGame"),
        @JsonSubTypes.Type(value = LeaveLobbyCommand.class,    name = "LeaveLobby"),
        @JsonSubTypes.Type(value = MoveTotemCommand.class,     name = "MoveTotem"),
        @JsonSubTypes.Type(value = ResolveActionsCommand.class, name = "ResolveActions"),
        @JsonSubTypes.Type(value = PongCommand.class, name = "Pong")
})

public sealed interface Command extends Message permits GameCommand, LobbyCommand {}

