package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.List;
import java.util.Map;

/**
 * Holds all the data needed to reconstruct a {@link it.polimi.ingsw.am02.server.model.Game}
 * from scratch during crash recovery.
 *
 * @param gameId      the game's unique identifier
 * @param seed        the random seed used to initialise the game (ensures deterministic replay)
 * @param nicknames   ordered list of player nicknames
 * @param chosenTotems mapping from nickname to chosen totem colour
 */
public record GameInitRecord(String gameId, long seed, List<String> nicknames, Map<String, Totem> chosenTotems) {}
