package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record UsernameResultEvent(String username, boolean isValid, String reason) implements Event {}