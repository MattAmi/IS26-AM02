package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

/** Notifies all players that a player's resource value has changed. */
public record PlayerResourceChangedEvent(String nickname, ResourceType resource, int newValue, int delta) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyPlayerResourceChanged(nickname, resource, newValue, delta);
    }
}