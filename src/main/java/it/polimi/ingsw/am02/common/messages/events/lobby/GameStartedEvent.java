package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record GameStartedEvent(String gameID) implements Event {}