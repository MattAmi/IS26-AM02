package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory that deserializes a {@link TurnOrderTile} from a Jackson {@link JsonNode}.
 */
public class TurnOrderTilesFactory {

    /**
     * Creates a {@link TurnOrderTile} from the given JSON node.
     *
     * @param node the JSON object containing {@code numPlayers}, {@code foodBonuses},
     *             and {@code prestigePointsMalus} arrays
     * @return the constructed {@link TurnOrderTile}
     */
    public TurnOrderTile createTurnOrderTile(JsonNode node) {
        int numPlayers = node.path("numPlayers").asInt();

        List<Integer> foodBonuses = new ArrayList<>();
        node.path("foodBonuses").forEach(n -> foodBonuses.add(n.asInt()));

        List<Integer> prestigePointsMalus = new ArrayList<>();
        node.path("prestigePointsMalus").forEach(n -> prestigePointsMalus.add(n.asInt()));

        return new TurnOrderTile(numPlayers, foodBonuses, prestigePointsMalus);
    }
}
