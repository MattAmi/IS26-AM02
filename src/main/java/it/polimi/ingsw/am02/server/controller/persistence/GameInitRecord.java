package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;
import java.util.Map;

public record GameInitRecord(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems) {}
