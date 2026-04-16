package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.common.enumerations.Era;

public class BuildingCard {

    private final String cardID;
    private final Era era;
    private final int buildingCost;
    private final int buildingPp;

    private final String effectType;
    private final JsonNode effectParams;
    // A JsonNode is like a variable that contains JSON in it

    public BuildingCard(String cardID, Era era, int buildingCost, int buildingPp, String effectType, JsonNode effectParams) {
        this.cardID = cardID;
        this.era = era;
        this.buildingCost = buildingCost;
        this.buildingPp = buildingPp;
        this.effectType = effectType;
        this.effectParams = effectParams;
    }

    public String getCardID() {
        return cardID;
    }

    public Era getEra() {
        return era;
    }

    public int getBuildingCost() {
        return buildingCost;
    }

    public int getBuildingPp() { return buildingPp; }

    public String getEffectType() {
        return effectType;
    }

    public JsonNode getEffectParams() {
        return effectParams;
    }

}