package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;

/**
 * Immutable snapshot of a single offer tile's state.
 * Included in {@link BoardSnapshot} and updated incrementally via
 * {@link it.polimi.ingsw.am02.common.messages.events.game.TotemPlacedEvent}
 * and {@link it.polimi.ingsw.am02.common.messages.events.game.TotemReturnedEvent}.
 *
 * @param tileID           the single-character identifier of this tile (e.g. {@code 'A'})
 * @param foodBonus        food points granted to the player on first action from this tile
 * @param upperChoosable   maximum cards the player may pick from the upper row
 * @param lowerChoosable   maximum cards the player may pick from the lower row
 * @param occupantNickname the nickname of the player currently on this tile, or {@code null} if empty
 */
public record OfferTileInfo(
        char tileID,
        int foodBonus,
        int upperChoosable,
        int lowerChoosable,
        String occupantNickname
) implements Serializable {}