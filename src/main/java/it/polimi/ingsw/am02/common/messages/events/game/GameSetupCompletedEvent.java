package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import java.util.List;
import java.util.Map;

/** Notifies all players of the initial board and player setup at game start. */
public record GameSetupCompletedEvent(Map<String, Totem> totemByPlayer, List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyGameSetupCompleted(totemByPlayer, turnOrder, initialFood, boardSnapshot);
    }
}