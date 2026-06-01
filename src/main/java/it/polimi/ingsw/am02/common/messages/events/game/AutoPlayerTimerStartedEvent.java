package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/**
 * Broadcast to all players (except the disconnected one) when the
 * per-player AutoPlayer grace timer is armed.
 *
 * @param nickname the disconnected player for whom the timer is running
 * @param seconds  the duration of the grace period in seconds
 */
public record AutoPlayerTimerStartedEvent(String nickname, long seconds) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyAutoPlayerTimerStarted(nickname, seconds);
    }
}