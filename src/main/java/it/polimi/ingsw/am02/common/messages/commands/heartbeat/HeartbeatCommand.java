package it.polimi.ingsw.am02.common.messages.commands.heartbeat;

import it.polimi.ingsw.am02.common.messages.commands.Command;

/**
 * Marker interface for heartbeat commands exchanged between client and server
 * over the Socket transport layer.
 *
 * <p>Subtypes:
 * <ul>
 *   <li>{@link PingCommand} — sent by the client to probe server liveness</li>
 *   <li>{@link PongCommand} — sent by the client in reply to a server
 *       {@link it.polimi.ingsw.am02.common.messages.events.heartbeat.PingEvent}</li>
 * </ul>
 *
 * <p>Both subtypes are handled at the transport layer before
 * {@link Command#apply} is ever called; their {@code apply} implementations
 * are therefore no-ops.
 */
public sealed interface HeartbeatCommand extends Command
        permits PingCommand, PongCommand {}