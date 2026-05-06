package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record PlayerResourceChangedEvent(String nickname, ResourceType resource, int newValue, int delta) implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updatePlayerResource(nickname, resource, newValue); }
}