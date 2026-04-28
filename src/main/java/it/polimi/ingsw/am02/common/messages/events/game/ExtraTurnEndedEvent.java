package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record ExtraTurnEndedEvent(String nickname) implements Event, GameEvent{}