package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event card that applies a game-wide effect when resolved.
 * Events can be round events (resolved once at the end of a round) or
 * final events (resolved only at the very end of the game).
 */
public class EventCard {

    private final String cardID;
    private final Era era;
    private final EventType type;
    private final Boolean isFinal;
    private final int priority;
    private final EventEffect eventEffect;

    /**
     * @param cardID      unique identifier (e.g. {@code "E_001"})
     * @param era         the era this event belongs to
     * @param type        the event type, used to match building observers
     * @param isFinal     {@code true} if this event is resolved only at game end
     * @param priority    resolution order among simultaneous events (lower = first)
     * @param eventEffect the effect applied to all players when this event fires
     */
    public EventCard(String cardID, Era era, EventType type, Boolean isFinal, int priority, EventEffect eventEffect) {
        this.cardID = cardID;
        this.era = era;
        this.type = type;
        this.isFinal = isFinal;
        this.priority = priority;
        this.eventEffect = eventEffect;
    }

    /** @return the unique card identifier */
    public String getID() {
        return cardID;
    }

    /** @return the era this event belongs to */
    public Era getEra() {
        return era;
    }

    /** @return the event type (used to notify matching building observers) */
    public EventType getType() {
        return type;
    }

    /** @return {@code true} if this is a final event, resolved only at game end */
    public Boolean isFinal() {
        return isFinal;
    }

    /** @return the priority used to order simultaneous event resolutions (lower = earlier) */
    public int getPriority() {
        return priority;
    }

    /**
     * Applies this event's effect to all players and notifies registered {@link EventObserver}s
     * before and after resolution.
     *
     * @param players        all players affected by the event
     * @param eventObservers building effects observing event resolutions
     * @return an {@link EffectOutcome} aggregating all resource deltas (from the event and observers)
     */
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