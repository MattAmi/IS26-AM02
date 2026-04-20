package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;

public record OfferTileInfo(
        char tileID,
        int foodBonus,
        int upperChoosable,
        int lowerChoosable,
        String occupantNickname
) implements Serializable {}