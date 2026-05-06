package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.messages.events.Event;
import java.util.List;

public record GameEndedEvent(List<String> winners, List<PlayerFinalScore> finalRankings) implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updateGameEnded(winners, finalRankings); }
}