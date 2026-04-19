package it.polimi.ingsw.am02.common.dto;

import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import java.io.Serializable;

public record ResourceDelta(
        String playerNickname,
        ResourceType resource,
        int newValue,
        int delta
) implements Serializable{}
