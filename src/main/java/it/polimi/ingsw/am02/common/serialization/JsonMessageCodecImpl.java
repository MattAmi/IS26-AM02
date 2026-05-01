package it.polimi.ingsw.am02.common.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am02.common.messages.Message;

public class JsonMessageCodecImpl implements JsonMessageCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public String encode(Message message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization Error: " + message.getClass().getSimpleName(), e);
        }
    }

    /**
     * Decodifica un messaggio JSON verso il tipo corretto.
     *
     * Il problema: {@link it.polimi.ingsw.am02.common.messages.Message} usa
     * {@code @JsonTypeInfo(Id.CLASS)}, ma le sue sotto-interfacce
     * {@link it.polimi.ingsw.am02.common.messages.events.Event} e
     * {@link it.polimi.ingsw.am02.common.messages.commands.Command}
     * usano {@code @JsonTypeInfo(Id.NAME)} con {@code @JsonSubTypes} espliciti.
     * Deserializzare da {@code Message.class} forza Jackson a usare Id.CLASS,
     * che non funziona con i nomi corti (es. "UpdatedLobbies").
     *
     * La soluzione: leggere il valore di "@type" dal JSON, e se è un nome
     * registrato in {@code Event} deserializzare da lì, altrimenti da {@code Command}.
     */
    @Override
    public Message decode(String json) {
        try {
            // Leggi il campo "@type" senza deserializzare il resto
            com.fasterxml.jackson.databind.JsonNode node = MAPPER.readTree(json);
            com.fasterxml.jackson.databind.JsonNode typeNode = node.get("@type");

            if (typeNode != null) {
                String typeName = typeNode.asText();
                // I Command sono registrati in Command/@JsonSubTypes con questi nomi
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

            // Fallback: prova come Command (ha anche "commandType")
            if (node.has("commandType")) {
                return MAPPER.treeToValue(node, it.polimi.ingsw.am02.common.messages.commands.Command.class);
            }

            throw new RuntimeException("Deserialization Error: campo '@type' assente in: " + json);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization Error: " + json, e);
        }
    }
}