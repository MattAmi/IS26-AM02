package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;

/**
 * Immutable breakdown of a player's final prestige-point score.
 * Included in {@link it.polimi.ingsw.am02.common.messages.events.game.GameEndedEvent}
 * for display in the end-game screen.
 *
 * @param nickname             the player's display name
 * @param totalPrestigePoints  the total prestige points accumulated at game end
 * @param ppBuilders           prestige points from Builder characters
 * @param ppBuildings          prestige points from owned buildings
 * @param ppInventors          prestige points from Inventor diversity bonus
 * @param ppArtists            prestige points from Artist characters
 */
public record PlayerFinalScore(
        String nickname,
        int totalPrestigePoints,
        int ppBuilders,
        int ppBuildings,
        int ppInventors,
        int ppArtists
) implements Serializable {}