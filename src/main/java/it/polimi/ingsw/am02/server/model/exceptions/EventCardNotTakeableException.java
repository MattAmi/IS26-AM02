package it.polimi.ingsw.am02.server.model.exceptions;

public class EventCardNotTakeableException extends GameRuleException {

    public EventCardNotTakeableException(String cardID) {
        super("Event cards cannot be taken by players: " + cardID);
    }
}