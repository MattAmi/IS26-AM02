package it.polimi.ingsw.am02.common.dto;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record LobbyInfo(
        String lobbyId,
        int expectedPlayers,
        List<String> currentPlayers,
        Map<String, Totem> chosenTotems
) implements Serializable {}