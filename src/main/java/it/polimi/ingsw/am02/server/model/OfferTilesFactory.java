package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Factory that deserializes an {@link OfferTile} from a Jackson {@link JsonNode}.
 */
public class OfferTilesFactory {

    /**
     * Creates an {@link OfferTile} from the given JSON node.
     *
     * @param node the JSON object representing one offer tile entry
     * @return the constructed {@link OfferTile}
     */
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
