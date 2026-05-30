package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players that a player's extra turn has ended. */
public record ExtraTurnEndedEvent(String nickname) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyExtraTurnEnded(nickname);
    }
}