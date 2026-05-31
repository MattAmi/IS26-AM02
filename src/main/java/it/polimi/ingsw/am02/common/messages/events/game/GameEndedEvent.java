package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import java.util.List;

/** Notifies all players that the game has ended and supplies the final scores. */
public record GameEndedEvent(List<String> winners, List<PlayerFinalScore> finalRankings) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGameEnded(winners, finalRankings);
    }
}