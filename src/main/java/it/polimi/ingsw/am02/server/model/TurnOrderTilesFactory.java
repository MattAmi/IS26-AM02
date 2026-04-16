package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class TurnOrderTilesFactory {

    public TurnOrderTile createTurnOrderTile(JsonNode node) {
        int numPlayers = node.path("numPlayers").asInt();

        List<Integer> foodBonuses = new ArrayList<>();
        node.path("foodBonuses").forEach(n -> foodBonuses.add(n.asInt()));

        List<Integer> prestigePointsMalus = new ArrayList<>();
        node.path("prestigePointsMalus").forEach(n -> prestigePointsMalus.add(n.asInt()));

        return new TurnOrderTile(numPlayers, foodBonuses, prestigePointsMalus);
    }
}
