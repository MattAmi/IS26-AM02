package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;

import java.util.List;

/** Notifies all players of the turn order for the upcoming round. */
public record TurnOrderEstablishedEvent(List<String> turnOrder) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyTurnOrderEstablished(turnOrder);
    }
}