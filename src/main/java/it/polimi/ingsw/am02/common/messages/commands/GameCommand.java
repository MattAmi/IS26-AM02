package it.polimi.ingsw.am02.common.messages.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Marker interface for commands sent during an active game.
 *
 * <p>Each game command carries the sender's {@link #nickname()} as a record
 * component. This field is <strong>not</strong> used for live request routing:
 * while a game is in progress the server resolves the sender from the
 * connection's {@code clientId -> nickname} mapping held by
 * {@link it.polimi.ingsw.am02.server.controller.ControllerManager}, so the
 * transport client sends the field blank.
 *
 * <p>The nickname is retained so that each command is self-descriptive once
 * serialized, in particular when written to the NDJSON command log by
 * {@link it.polimi.ingsw.am02.server.controller.persistence.CommandLogger}
 * (where {@link it.polimi.ingsw.am02.server.controller.GameController} fills it
 * with the real sender). Crash-recovery replay, however, reads the sender from
 * the log record's <em>envelope</em> nickname
 * ({@link it.polimi.ingsw.am02.server.controller.persistence.<CommandRecord#nickname()}),
 * not from this field: the value carried here is redundant but kept to preserve
 * the serialized command shape and backward compatibility of existing logs.
 */
public sealed interface GameCommand extends Command
        permits MoveTotemCommand, ResolveActionsCommand {

    /**
     * @return the nickname the command is attributed to; populated server-side
     *         (by the AutoPlayer and the command logger) and left blank by the
     *         transport client. Not consulted for routing or replay — see the
     *         interface-level documentation.
     */
    String nickname();
}
