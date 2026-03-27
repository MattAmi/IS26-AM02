package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.EventType;

public interface EventObserver {
    public void EventStart(EventType eventType);
    default void EventEnd(EventType eventType){

    }
}
