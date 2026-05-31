package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import java.util.List;

/** Notifies all players of the card-row diff at the start of a new round. */
public record BoardUpdatedEvent(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards,
                                List<String> movedToLowerRow, int deckRemainingCount) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyBoardUpdated(newUpperRow, newLowerRow, discardedCards, movedToLowerRow, deckRemainingCount);
    }
}