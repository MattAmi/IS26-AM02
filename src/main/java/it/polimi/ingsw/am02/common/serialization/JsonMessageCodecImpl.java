package it.polimi.ingsw.am02.common.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;

/**
 * Jackson-based implementation of {@link JsonMessageCodec}.
 *
 * <p>Serialization delegates directly to {@link ObjectMapper#writeValueAsString}.
 *
 * <p>Deserialization exploits the fact that {@link Command} and {@link Event}
 * use different Jackson type-info property names:
 * <ul>
 *   <li>{@link Command} — {@code @JsonTypeInfo(property = "commandType")}</li>
 *   <li>{@link Event}   — {@code @JsonTypeInfo(property = "@type")}</li>
 * </ul>
 * The presence of {@code "commandType"} in the JSON object is therefore a
 * reliable, zero-maintenance discriminator that requires no hardcoded name set
 * and automatically covers every future {@link Command} subtype.
 */
public class JsonMessageCodecImpl implements JsonMessageCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Serializes {@code message} to a JSON string.
     *
     * @param message the message to serialize; must not be {@code null}
     * @return the JSON representation of {@code message}
     * @throws RuntimeException if Jackson serialization fails
     */
    @Override
    public String encode(Message message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Serialization error: " + message.getClass().getSimpleName(), e);
        }
    }

    /**
     * Deserializes a JSON string into the appropriate {@link Message} subtype.
     *
     * <p>The discriminator is the presence of the {@code "commandType"} field:
     * if present, the message is a {@link Command}; otherwise it is an
     * {@link Event}. This works because {@link Command} uses
     * {@code @JsonTypeInfo(property = "commandType")} while {@link Event} uses
     * {@code @JsonTypeInfo(property = "@type")}, so the two namespaces are
     * disjoint by construction.
     *
     * @param json the JSON string to deserialize; must not be {@code null}
     * @return the deserialized {@link Message}
     * @throws RuntimeException if the JSON is malformed or Jackson
     *                          deserialization fails
     */
    @Override
    public Message decode(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);

            if (node.has("commandType")) {
                return MAPPER.treeToValue(node, Command.class);
            } else {
                return MAPPER.treeToValue(node, Event.class);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization error: " + json, e);
        }
    }
}