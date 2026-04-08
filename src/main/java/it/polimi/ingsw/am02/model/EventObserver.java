package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.EventType;

public interface EventObserver {
    default void EventStart(EventType eventType){}
    default void EventPostResolution(EventType eventType){}
    default void EventEnd(EventType eventType){}

}
