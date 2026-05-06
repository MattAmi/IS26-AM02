package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.client.model.GameModel;

public record GameRecoveryFailedEvent() implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updateGameRecoveryFailed(); }
}
