package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.messages.events.Event;
import java.util.List;

public record PhaseChangedEvent(PhaseType phase, String currentPlayer, List<String> resolutionOrder)implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updateCurrentPhase(phase, currentPlayer, resolutionOrder); }
}