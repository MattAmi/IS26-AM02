package it.polimi.ingsw.am02.common.messages;

import java.io.Serializable;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;

/**
 * Root of the sealed hierarchy of all messages exchanged between client and server.
 * Extends {@link Serializable} so that RMI can natively transport these objects,
 * and so that the Socket {@code MessageCodec} can encode/decode them reliably.
 *
 * <p>Every concrete message is either a {@link Command} (client → server)
 * or an {@link Event} (server → client). No other message category exists.
 */
public sealed interface Message extends Serializable
        permits Command, Event {
}
