package it.polimi.ingsw.am02.common.messages.events.heartbeat;

import it.polimi.ingsw.am02.common.messages.events.Event;

/**
 * Marker interface for heartbeat events exchanged between server and client
 * over the Socket transport layer.
 *
 * <p>Subtypes:
 * <ul>
 *   <li>{@link PingEvent} — sent by the server to probe client liveness</li>
 *   <li>{@link PongEvent} — sent by the server in reply to a client
 *       {@link it.polimi.ingsw.am02.common.messages.commands.heartbeat.PingCommand}</li>
 * </ul>
 *
 * <p>Both subtypes are handled at the transport layer before
 * {@link Event#apply} is ever called; their {@code apply} implementations
 * are therefore no-ops.
 */
public sealed interface HeartbeatEvent extends Event
        permits PingEvent, PongEvent {}