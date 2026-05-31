package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Immutable snapshot of the full board state at a given point in time.
 * Sent to clients during game setup ({@link it.polimi.ingsw.am02.common.messages.events.game.GameSetupCompletedEvent})
 * and used by the server-side to initialize the AutoPlayer view.
 *
 * @param upperRowCards       card IDs currently in the upper (character/event) row
 * @param lowerRowCards       card IDs currently in the lower (character/event) row
 * @param upperRowBuildings   building card IDs in the upper building market row
 * @param lowerRowBuildings   building card IDs in the lower building market row
 * @param offerTiles          the current state of each offer tile on the track
 * @param turnOrderSlots      the current state of each slot on the turn-order tile
 * @param tribuDeckSize       the number of cards remaining in the tribe deck
 */
public record BoardSnapshot(
        List<String> upperRowCards,
        List<String> lowerRowCards,
        List<String> upperRowBuildings,
        List<String> lowerRowBuildings,
        List<OfferTileInfo> offerTiles,
        List<TurnOrderSlotInfo> turnOrderSlots,
        int tribuDeckSize
) implements Serializable {}