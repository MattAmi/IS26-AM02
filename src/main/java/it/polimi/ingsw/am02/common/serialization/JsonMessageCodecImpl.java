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
            // Jackson will read the annotation on Message and will add "@type"
            return MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization Error: " + message.getClass().getSimpleName(), e);
        }
    }

    @Override
    public Message decode(String json) {
        try {
            // Jackson will read "@type" and will automatically create the correct class
            return MAPPER.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization Error: " + json, e);
        }
    }
}