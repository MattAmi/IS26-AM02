package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record ExtraTurnEndedEvent(String nickname) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyExtraTurnEnded(nickname);
    }
}