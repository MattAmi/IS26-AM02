package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.JsonNode;

public class OfferTilesFactory {

    public OfferTile createOfferTile(JsonNode node) {
        return new OfferTile(
                node.path("tileID").asText().charAt(0),
                node.path("minPlayers").asInt(),
                node.path("gainedFood").asInt(),
                node.path("numUpperChoosable").asInt(),
                node.path("numLowerChoosable").asInt()
        );
    }

}
