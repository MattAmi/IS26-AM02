package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;

/**
 * Observer notified at each stage of an event card's resolution lifecycle.
 * Implemented by building effects that interact with specific event types
 * (e.g. granting temporary bonuses or immunity during an event).
 *
 * <p>All three methods have default no-op implementations so that concrete
 * observers only need to override the stages they care about.
 */
public interface EventObserver {

    /**
     * Called immediately before the event's main effect is applied.
     * Use this to apply temporary bonuses or flags that must be in place
     * when the event resolves (e.g. extra shaman stars, immunity).
     *
     * @param eventType the type of event that is about to be resolved
     * @return an {@link EffectOutcome} with any pre-event resource changes, or empty
     */
    default EffectOutcome eventStart(EventType eventType) { return EffectOutcome.empty(); }

    /**
     * Called immediately after the event's main effect has been applied,
     * but before {@link #eventEnd}. Use this to apply bonuses that depend on
     * the event's outcome (e.g. multiplying a majority bonus).
     *
     * @param eventType the type of event that just resolved
     * @return an {@link EffectOutcome} with any post-resolution resource changes, or empty
     */
    default EffectOutcome eventPostResolution(EventType eventType) { return EffectOutcome.empty(); }

    /**
     * Called after the event and all post-resolution effects have been applied.
     * Use this to clean up temporary state set in {@link #eventStart}
     * (e.g. removing extra shaman stars or clearing immunity flags).
     *
     * @param eventType the type of event that has fully resolved
     * @return an {@link EffectOutcome} with any cleanup resource changes, or empty
     */
    default EffectOutcome eventEnd(EventType eventType) { return EffectOutcome.empty(); }
}
