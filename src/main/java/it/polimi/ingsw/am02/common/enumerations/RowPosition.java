package it.polimi.ingsw.am02.common.enumerations;

/**
 * Identifies which card row a card was drawn from or belongs to.
 * Used in {@link it.polimi.ingsw.am02.common.dto.ResourceDelta} and
 * {@link it.polimi.ingsw.am02.common.messages.events.game.CardTakenEvent}.
 */
public enum RowPosition {
    UPPER, LOWER
}
