package it.polimi.ingsw.am02.common.messages.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;
import it.polimi.ingsw.am02.common.messages.Message;

/**
 * Sealed interface for all client-to-server commands.
 * Each concrete record carries its payload as record components and
 * implements {@link #apply} to dispatch itself to the correct
 * {@link it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager} method,
 * eliminating switch logic in the transport handlers.
 *
 * <p>Subtypes are split into {@link LobbyCommand} (pre-game actions),
 * {@link GameCommand} (in-game actions), and {@link ReconnectCommand}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SetUsernameCommand.class,     name = "SetUsername"),
        @JsonSubTypes.Type(value = CreateLobbyCommand.class,    name = "CreateLobby"),
        @JsonSubTypes.Type(value = JoinLobbyCommand.class,      name = "JoinLobby"),
        @JsonSubTypes.Type(value = SelectTotemCommand.class,    name = "SelectTotem"),
        @JsonSubTypes.Type(value = StartGameCommand.class,      name = "StartGame"),
        @JsonSubTypes.Type(value = LeaveLobbyCommand.class,     name = "LeaveLobby"),
        @JsonSubTypes.Type(value = PongCommand.class,           name = "Pong"),
        @JsonSubTypes.Type(value = MoveTotemCommand.class,      name = "MoveTotemCommand"),
        @JsonSubTypes.Type(value = ResolveActionsCommand.class, name = "ResolveActionsCommand"),
        @JsonSubTypes.Type(value = ReconnectCommand.class,      name = "ReconnectCommand")
})
public sealed interface Command extends Message permits GameCommand, LobbyCommand, ReconnectCommand {
    /**
     * Applies this command to the given {@link VirtualControllerManager}.
     * Implements the Inversion of Control pattern: each command knows which
     * manager method to invoke, keeping {@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler}
     * free of any switch logic.
     *
     * @param manager  the controller manager to dispatch to
     * @param clientId the identifier of the client that sent this command
     */
    void apply(VirtualControllerManager manager, String clientId);
}