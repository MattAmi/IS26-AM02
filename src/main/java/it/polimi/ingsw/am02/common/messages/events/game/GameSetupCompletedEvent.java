package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import java.util.List;
import java.util.Map;

public record GameSetupCompletedEvent(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGameSetupCompleted(turnOrder, initialFood, boardSnapshot);
    }
}