package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record BoardSnapshot(
        List<String> upperRowCards,
        List<String> lowerRowCards,
        List<String> upperRowBuildings,
        List<String> lowerRowBuildings,
        List<OfferTileInfo> offerTiles,
        List<String> turnOrderPositions
) implements Serializable {}