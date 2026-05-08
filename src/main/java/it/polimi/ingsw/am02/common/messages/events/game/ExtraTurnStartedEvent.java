package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record ExtraTurnStartedEvent(String nickname, int remainingUpper, int remainingLower) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyExtraTurnStarted(nickname, remainingUpper, remainingLower);
    }
}