package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record PlayerLimitsInitializedEvent(String nickname, int remainingUpper, int remainingLower)implements GameEvent {}