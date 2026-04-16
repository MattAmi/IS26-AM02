package it.polimi.ingsw.am02.model;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.model.enumerations.EventType;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class EventFactory {
    private final Map<EventType, Function<JsonNode, EventEffect>> effectRegistry;

    public EventFactory(){
        this.effectRegistry = new EnumMap<>(EventType.class);
        setUpRegistry();
    }

    private void setUpRegistry(){
        effectRegistry.put(EventType.SUSTENANCE, node ->
                new SustenanceEffect(
                        node.path("penaltyPerUnfed").asInt())
        );

        effectRegistry.put(EventType.SHAMANIC_RITUAL, node ->
                new ShamanicRitualEffect(
                        node.path("majorityBonus").asInt(),
                        node.path("minorityMalus").asInt())
        );

        effectRegistry.put(EventType.HUNT, node ->
                new HuntEffect(
                        node.path("foodPerHunter").asInt(),
                        node.path("ppPerHunter").asInt())
        );

        effectRegistry.put(EventType.CAVE_PAINTINGS, node ->
                new CavePaintingsEffect(
                        node.path("minArtists").asInt(),
                        node.path("ppMalusIfFailed").asInt(),
                        node.path("bonusPerArtist").asInt())
        );

    }

    public EventCard createEvent(JsonNode node){
        String cardID = node.path("cardID").asText();
        Era era = Era.valueOf(node.path("era").asText());
        EventType type = EventType.valueOf(node.path("type").asText());
        boolean isFinal = node.path("isFinal").asBoolean();
        int priority = node.path("priority").asInt();

        EventEffect eventEffect = effectRegistry
                .get(type)
                .apply(node);

        return new EventCard(cardID, era, type, isFinal, priority, eventEffect);

    }

}
