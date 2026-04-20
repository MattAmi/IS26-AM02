package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.common.messages.events.Event;
import java.util.List;

public record BoardUpdatedEvent(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) implements Event {}