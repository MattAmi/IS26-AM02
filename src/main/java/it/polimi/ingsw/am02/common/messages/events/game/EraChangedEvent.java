package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

import java.util.List;

/** Notifies all players that the game has entered a new era and the building market changed. */
public record EraChangedEvent(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings,
                              List<String> discardedBuildings) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyEraChanged(newEra, newUpperRowBuildings, newLowerRowBuildings, discardedBuildings);
    }
}