package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;

public record PlayerFinalScore(
        String nickname,
        int totalPrestigePoints,
        int ppBuilders,
        int ppBuildings,
        int ppInventors,
        int ppArtists
) implements Serializable {}