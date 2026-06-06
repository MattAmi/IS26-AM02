package it.polimi.ingsw.am02.common.messages.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.heartbeat.HeartbeatCommand;
import it.polimi.ingsw.am02.common.messages.commands.heartbeat.PingCommand;
import it.polimi.ingsw.am02.common.messages.commands.heartbeat.PongCommand;

/**
 * Sealed interface for all client-to-server commands.
 * Each concrete record carries its payload as record components and
 * implements {@link #apply} to dispatch itself to the correct
 * {@link VirtualControllerManager} method, eliminating switch logic
 * in the transport handlers.
 *
 * <p>Subtypes are split into:
 * <ul>
 *   <li>{@link LobbyCommand} — pre-game actions</li>
 *   <li>{@link GameCommand} — in-game actions</li>
 *   <li>{@link ReconnectCommand} — reconnection after disconnection</li>
 *   <li>{@link HeartbeatCommand} — liveness probes ({@link PingCommand},
 *       {@link PongCommand}), handled at the transport layer</li>
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SetUsernameCommand.class,      name = "SetUsername"),
        @JsonSubTypes.Type(value = CreateLobbyCommand.class,      name = "CreateLobby"),
        @JsonSubTypes.Type(value = JoinLobbyCommand.class,        name = "JoinLobby"),
        @JsonSubTypes.Type(value = SelectTotemCommand.class,      name = "SelectTotem"),
        @JsonSubTypes.Type(value = StartGameCommand.class,        name = "StartGame"),
        @JsonSubTypes.Type(value = LeaveLobbyCommand.class,       name = "LeaveLobby"),
        @JsonSubTypes.Type(value = MoveTotemCommand.class,        name = "MoveTotemCommand"),
        @JsonSubTypes.Type(value = ResolveActionsCommand.class,   name = "ResolveActionsCommand"),
        @JsonSubTypes.Type(value = ReconnectCommand.class,        name = "ReconnectCommand"),
        @JsonSubTypes.Type(value = PingCommand.class,             name = "Ping"),
        @JsonSubTypes.Type(value = PongCommand.class,             name = "Pong")
})
public sealed interface Command extends Message
        permits GameCommand, LobbyCommand, ReconnectCommand, HeartbeatCommand {

    /**
     * Applies this command to the given {@link VirtualControllerManager}.
     * Implements the Inversion of Control pattern: each command knows which
     * manager method to invoke, keeping
     * {@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler}
     * free of any switch logic.
     *
     * <p>Heartbeat subtypes ({@link PingCommand}, {@link PongCommand}) are
     * intercepted before this method is called; invoking {@code apply} on them
     * throws {@link UnsupportedOperationException}.
     *
     * @param manager  the controller manager to dispatch to
     * @param clientId the identifier of the client that sent this command
     */
    void apply(VirtualControllerManager manager, String clientId);
}