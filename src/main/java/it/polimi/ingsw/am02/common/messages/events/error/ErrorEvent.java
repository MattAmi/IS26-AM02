package it.polimi.ingsw.am02.common.messages.events.error;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record ErrorEvent(String errorMessage) implements Event {}