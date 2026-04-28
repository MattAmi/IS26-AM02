package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record TotemReturnedEvent(String nickname, int turnOrderPosition) implements GameEvent {}