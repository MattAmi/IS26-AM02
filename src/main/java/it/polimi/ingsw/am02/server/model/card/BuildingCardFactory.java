package it.polimi.ingsw.am02.server.model.card;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.common.enumerations.Era;

/**
 * Factory that deserializes a {@link BuildingCard} from a Jackson {@link JsonNode}.
 */
public class BuildingCardFactory {

    /**
     * Creates a {@link BuildingCard} from the given JSON node.
     *
     * @param node the JSON object representing one building card entry
     * @return the constructed {@link BuildingCard}
     */
    public BuildingCard createBuilding(JsonNode node) {

        String cardID = node.path("cardID").asText();
        Era era = Era.valueOf(node.path("era").asText());
        int buildingCost = node.path("buildingCost").asInt();
        int buildingPp = node.path("buildingPp").asInt();
        String effectType = node.path("effectType").asText();
        JsonNode effectParams = node.path("effectParams");

        return new BuildingCard(cardID, era, buildingCost, buildingPp, effectType, effectParams);
    }

}
