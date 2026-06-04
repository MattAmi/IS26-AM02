package it.polimi.ingsw.am02.common.serialization;

import it.polimi.ingsw.am02.common.messages.Message;

/**
 * Codec for serializing and deserializing {@link Message} objects to and from
 * a string representation.
 *
 * <p>The concrete implementation determines the wire format (e.g. JSON).
 * Used by the Socket transport to encode outbound {@link
 * it.polimi.ingsw.am02.common.messages.events.Event} records and decode
 * inbound {@link it.polimi.ingsw.am02.common.messages.commands.Command} records.
 */
public interface MessageCodec {

    /**
     * Serializes a {@link Message} to its string representation.
     *
     * @param message the message to serialize; must not be {@code null}
     * @return the encoded string
     * @throws RuntimeException if serialization fails
     */
    String encode(Message message);

    /**
     * Deserializes a string into the appropriate {@link Message} subtype.
     *
     * @param string the encoded message string; must not be {@code null}
     * @return the deserialized {@link Message}
     * @throws RuntimeException if deserialization fails or the type cannot be determined
     */
    Message decode(String string);
}