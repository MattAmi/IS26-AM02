package it.polimi.ingsw.am02.server.model.enumerations;

/**
 * Enumerates the four types of events that can appear on event cards.
 * Each value is matched by building effects that implement
 * {@link it.polimi.ingsw.am02.server.model.listeners.EventObserver}:
 *
 * <ul>
 *   <li>{@link #SUSTENANCE}      — players pay food per character or lose prestige points</li>
 *   <li>{@link #SHAMANIC_RITUAL} — majority/minority of shaman stars earns or costs prestige</li>
 *   <li>{@link #HUNT}            — players gain food and prestige per Hunter character</li>
 *   <li>{@link #CAVE_PAINTINGS}  — players meeting the Artist threshold gain prestige; others lose it</li>
 * </ul>
 */
public enum EventType {
    SUSTENANCE,SHAMANIC_RITUAL,HUNT,CAVE_PAINTINGS
}
