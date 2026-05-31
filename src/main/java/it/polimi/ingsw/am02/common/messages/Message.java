package it.polimi.ingsw.am02.common.messages;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;


/**
 * Root sealed interface for all network messages exchanged between
 * client and server. Subtypes are either {@link Command} (client → server)
 * or {@link Event} (server → client).
 *
 * <p>Jackson polymorphic deserialization is configured via
 * {@code @JsonTypeInfo(Id.CLASS)} at this level, but each subtype
 * overrides it with {@code @JsonTypeInfo(Id.NAME)} for compact wire names.
 * See {@link it.polimi.ingsw.am02.common.serialization.JsonMessageCodecImpl}
 * for the resolution strategy.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public sealed interface Message extends Serializable
        permits Command, Event {
}
