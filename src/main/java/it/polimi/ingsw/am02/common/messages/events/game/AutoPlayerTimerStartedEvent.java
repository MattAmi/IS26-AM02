package it.polimi.ingsw.am02.common.messages.events.game;

/**
 * Broadcast to all players (except the disconnected one) when the
 * per-player AutoPlayer grace timer is armed.
 *
 * @param nickname the disconnected player for whom the timer is running
 */
public record AutoPlayerTimerStartedEvent(String nickname) implements GameEvent {}
