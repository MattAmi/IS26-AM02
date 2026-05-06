package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import java.util.List;

public record GameEndedEvent(List<String> winners, List<PlayerFinalScore> finalRankings) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGameEnded(winners, finalRankings);
    }
}