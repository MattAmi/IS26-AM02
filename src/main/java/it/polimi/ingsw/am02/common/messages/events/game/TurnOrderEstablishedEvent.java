package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.messages.events.Event;
import java.util.List;

public record TurnOrderEstablishedEvent(List<String> turnOrder) implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updateTurnOrder(turnOrder); }
}