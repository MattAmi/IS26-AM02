package it.polimi.ingsw.am02.common.dto;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of a lobby's current state, broadcast to all
 * pre-lobby clients whenever the lobby list changes.
 *
 * @param lobbyId         the unique identifier of this lobby
 * @param expectedPlayers the total number of players this lobby waits for (2–5)
 * @param currentPlayers  ordered list of nicknames already in the lobby
 *                        (empty string if the slot is taken but the player has not set a name yet)
 * @param chosenTotems    mapping from nickname to chosen totem for players who have already selected one
 */
public record LobbyInfo(
        String lobbyId,
        int expectedPlayers,
        List<String> currentPlayers,
        Map<String, Totem> chosenTotems
) implements Serializable {}