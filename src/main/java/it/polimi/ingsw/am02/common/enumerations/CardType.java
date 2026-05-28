package it.polimi.ingsw.am02.common.enumerations;

/**
 * Distinguishes between the two types of cards that players can acquire from the board.
 *
 * <ul>
 *   <li>{@link #CHARACTER} — character cards drawn from the upper or lower tribe row</li>
 *   <li>{@link #BUILDING}  — building cards purchased from the building market rows</li>
 * </ul>
 */
public enum CardType {
    CHARACTER, BUILDING
}
