package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players of the active player's updated remaining pick counts. */
public record PlayerLimitsUpdatedEvent(String nickname, int remainingUpper, int remainingLower) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyPlayerLimitsUpdated(nickname, remainingUpper, remainingLower);
    }
}