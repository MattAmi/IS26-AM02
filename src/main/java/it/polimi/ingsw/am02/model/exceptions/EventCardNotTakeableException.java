package it.polimi.ingsw.am02.model.exceptions;

public class EventCardNotTakeableException extends RuntimeException {

    public EventCardNotTakeableException(String cardID) {
        super("Event cards cannot be taken by players: " + cardID);
    }
}