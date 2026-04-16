package it.polimi.ingsw.am02.model;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.common.enumerations.Era;

public class BuildingCardFactory {

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
