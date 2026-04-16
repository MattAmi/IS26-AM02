package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.model.enumerations.EventType;

public interface EventObserver {
    default void EventStart(EventType eventType){}
    default void EventPostResolution(EventType eventType){}
    default void EventEnd(EventType eventType){}

}
