package it.polimi.ingsw.am02.server.model.exceptions;

/**
 * Thrown when a player's selection includes an event card.
 * Event cards are resolved automatically by the game and cannot be
 * taken by players during action resolution.
 */
public class EventCardNotTakeableException extends GameRuleException {

    /**
     * @param cardID the ID of the event card the player attempted to take
     */
    public EventCardNotTakeableException(String cardID) {
        super("Event cards cannot be taken by players: " + cardID);
    }
}