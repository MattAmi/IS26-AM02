package it.polimi.ingsw.am02.common.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am02.common.messages.Message;

/**
 * Jackson-based implementation of {@link JsonMessageCodec}.
 *
 * <p>Serialization delegates directly to {@link ObjectMapper#writeValueAsString}.
 * Deserialization inspects the {@code @type} field to decide whether to
 * deserialize into a {@link it.polimi.ingsw.am02.common.messages.commands.Command}
 * or an {@link it.polimi.ingsw.am02.common.messages.events.Event}, working around
 * the conflict between {@code @JsonTypeInfo(Id.CLASS)} on {@link Message} and
 * {@code @JsonTypeInfo(Id.NAME)} on its sub-interfaces.
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
            throw new RuntimeException("Serialization Error: " + message.getClass().getSimpleName(), e);
        }
    }

    /**
     * Deserializes a JSON string into the appropriate {@link Message} subtype.
     *
     * <p>{@link Message} uses {@code @JsonTypeInfo(Id.CLASS)}, while its
     * sub-interfaces {@link it.polimi.ingsw.am02.common.messages.events.Event}
     * and {@link it.polimi.ingsw.am02.common.messages.commands.Command} use
     * {@code @JsonTypeInfo(Id.NAME)} with explicit {@code @JsonSubTypes}.
     * Deserializing directly from {@code Message.class} would force Jackson to
     * use {@code Id.CLASS}, which is incompatible with short names such as
     * {@code "UpdatedLobbies"}.
     *
     * <p>To work around this, the method reads the {@code @type} field from the
     * raw JSON tree and dispatches to {@code Command.class} if the value matches
     * a known command name, or to {@code Event.class} otherwise.
     *
     * @param json the JSON string to deserialize; must contain a valid {@code @type} field
     * @return the deserialized {@link Message}
     * @throws RuntimeException if the JSON is malformed, the {@code @type} field is
     *                          absent, or Jackson deserialization fails
     */
    @Override
    public Message decode(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = MAPPER.readTree(json);
            com.fasterxml.jackson.databind.JsonNode typeNode = node.get("@type");

            if (typeNode != null) {
                String typeName = typeNode.asText();
                java.util.Set<String> commandNames = java.util.Set.of(
                        "SetUsername", "CreateLobby", "JoinLobby", "SelectTotem",
                        "StartGame", "LeaveLobby", "MoveTotem", "ResolveActions",
                        "Pong", "Reconnect"
                );
                if (commandNames.contains(typeName)) {
                    return MAPPER.treeToValue(node, it.polimi.ingsw.am02.common.messages.commands.Command.class);
                } else {
                    return MAPPER.treeToValue(node, it.polimi.ingsw.am02.common.messages.events.Event.class);
                }
            }

            if (node.has("commandType")) {
                return MAPPER.treeToValue(node, it.polimi.ingsw.am02.common.messages.commands.Command.class);
            }

            throw new RuntimeException("Deserialization Error: campo '@type' assente in: " + json);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization Error: " + json, e);
        }
    }
}