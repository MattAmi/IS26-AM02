package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record GameAbortedEvent(String lastManStanding) implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updateGameAborted(lastManStanding); }
}
