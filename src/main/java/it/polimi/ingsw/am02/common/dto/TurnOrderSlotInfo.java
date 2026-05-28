package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;

/**
 * Immutable snapshot of a single slot on the turn-order tile.
 * Included in {@link BoardSnapshot} and updated when totems are placed or returned.
 *
 * @param occupantNickname   the nickname of the player in this slot, or {@code null} if empty
 * @param foodBonus          food reward (positive) or penalty (negative) for this slot position
 * @param prestigePointsMalus prestige-point penalty applied if the player cannot pay the food penalty
 *                            (stored as a negative value)
 */
public record TurnOrderSlotInfo(
        String occupantNickname,
        int foodBonus,
        int prestigePointsMalus
) implements Serializable {}
