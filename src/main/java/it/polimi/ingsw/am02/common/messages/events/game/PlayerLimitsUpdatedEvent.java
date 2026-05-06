package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record PlayerLimitsUpdatedEvent(String nickname, int remainingUpper, int remainingLower) implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updatePlayerLimits(nickname, remainingUpper, remainingLower); }
}