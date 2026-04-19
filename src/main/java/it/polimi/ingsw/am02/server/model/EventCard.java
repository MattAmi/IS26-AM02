package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;

import java.util.ArrayList;
import java.util.List;

public class EventCard {

    //Attributi
    private final String cardID;
    private final Era era;
    private final EventType type;
    private final Boolean isFinal;
    private final int priority;
    private final EventEffect eventEffect;

    //Costruttore
    public EventCard(String cardID, Era era, EventType type, Boolean isFinal, int priority, EventEffect eventEffect) {
        this.cardID = cardID;
        this.era = era;
        this.type = type;
        this.isFinal = isFinal;
        this.priority = priority;
        this.eventEffect = eventEffect;
    }

    //Metodi
    public String getID() {
        return cardID;
    }

    public Era getEra() {
        return era;
    }

    public EventType getType() {
        return type;
    }

    public Boolean isFinal() {
        return isFinal;
    }

    public int getPriority() {
        return priority;
    }

    public EffectOutcome applyEventEffect(List<Player> players, List<EventObserver> eventObservers) {

        EffectOutcome mainOutcome = eventEffect.applyEffect(players);

        List<ResourceDelta> allDeltas = new ArrayList<>(mainOutcome.resourceDeltas());

        for (EventObserver observer : eventObservers) {
            EffectOutcome observerOutcome = observer.eventPostResolution(this.type);

            if (observerOutcome != null && !observerOutcome.isEmpty()) {
                allDeltas.addAll(observerOutcome.resourceDeltas());
            }
        }

        return new EffectOutcome(allDeltas);
    }

}