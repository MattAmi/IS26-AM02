package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;
import java.util.List;

public record BoardSnapshot(
        List<String> upperRowCards,
        List<String> lowerRowCards,
        List<String> upperRowBuildings,
        List<String> lowerRowBuildings,
        List<OfferTileInfo> offerTiles,
        List<TurnOrderSlotInfo> turnOrderSlots,
        int tribuDeckSize
) implements Serializable {}