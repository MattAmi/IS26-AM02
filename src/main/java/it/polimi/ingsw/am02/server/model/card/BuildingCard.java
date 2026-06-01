package it.polimi.ingsw.am02.server.model.card;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.server.model.effect.BuildingEffect;
import it.polimi.ingsw.am02.server.model.effect.BuildingFactory;

/**
 * Represents an immutable building card template as loaded from JSON.
 * A building card defines the era it belongs to, its food cost, the base prestige
 * points it awards, and the type and parameters of its active {@link BuildingEffect}.
 */
public class BuildingCard {

    private final String cardID;
    private final Era era;
    private final int buildingCost;
    private final int buildingPp;

    private final String effectType;
    private final JsonNode effectParams;
    // A JsonNode is like a variable that contains JSON in it

    /**
     * @param cardID       unique identifier (e.g. {@code "B_020"})
     * @param era          the era this building belongs to ({@link Era#I}, {@link Era#II}, or {@link Era#III})
     * @param buildingCost the food cost to purchase this building
     * @param buildingPp   the base prestige points awarded when this building is acquired
     * @param effectType   the string key identifying the building effect (matched by {@link BuildingFactory})
     * @param effectParams the JSON parameters passed to the effect factory
     */
    public BuildingCard(String cardID, Era era, int buildingCost, int buildingPp, String effectType, JsonNode effectParams) {
        this.cardID = cardID;
        this.era = era;
        this.buildingCost = buildingCost;
        this.buildingPp = buildingPp;
        this.effectType = effectType;
        this.effectParams = effectParams;
    }

    /** @return the unique card identifier */
    public String getCardID() {
        return cardID;
    }

    /** @return the era this building card belongs to */
    public Era getEra() {
        return era;
    }

    /** @return the food cost to purchase this building */
    public int getBuildingCost() {
        return buildingCost;
    }

    /** @return the base prestige points awarded when this building is acquired */
    public int getBuildingPp() { return buildingPp; }

    /** @return the effect type key used by {@link BuildingFactory} to instantiate the correct effect */
    public String getEffectType() {
        return effectType;
    }

    /** @return the raw JSON parameters for the building effect */
    public JsonNode getEffectParams() {
        return effectParams;
    }

}