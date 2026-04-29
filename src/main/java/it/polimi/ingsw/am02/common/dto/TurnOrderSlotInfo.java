package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;

public record TurnOrderSlotInfo(
        String occupantNickname,
        int foodBonus,
        int prestigePointsMalus
) implements Serializable {}
