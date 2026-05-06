package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record PlayerDisconnectedEvent(String nickname) implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updatePlayerDisconnected(nickname); }
}