package it.polimi.ingsw.am02.model;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.model.Enumerations.Era;

public class BuildingCardFactory {

    public BuildingCard createBuilding(JsonNode node) {

        String cardID = node.get("cardID").asText();
        Era era = Era.valueOf(node.get("era").asText());
        int buildingCost = node.get("buildingCost").asInt();
        int buildingPp = node.get("buildingPp").asInt();
        String effectType = node.get("effectType").asText();
        JsonNode effectParams = node.get("effectParams");

        return new BuildingCard(cardID, era, buildingCost, buildingPp, effectType, effectParams);
    }

}
