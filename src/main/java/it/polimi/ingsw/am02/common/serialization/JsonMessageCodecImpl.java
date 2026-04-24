package it.polimi.ingsw.am02.common.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;

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

    @Override
    public Message decode(String json) {
        try {
            //"@type" is used by Jackson to know which concrete class is to be istanced.
            if (json.contains("\"@type\"")) {
                try {
                    return MAPPER.readValue(json, Command.class);
                } catch (Exception e) {
                    return MAPPER.readValue(json, Event.class);
                }
            }
            throw new RuntimeException("JSON message without a @type field: " + json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization Error: " + json, e);
        }
    }
}