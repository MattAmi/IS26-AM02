package it.polimi.ingsw.am02.common.messages.events.game;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.common.messages.events.Event;
import java.util.List;

public record EraChangedEvent(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) implements Event, GameEvent {}