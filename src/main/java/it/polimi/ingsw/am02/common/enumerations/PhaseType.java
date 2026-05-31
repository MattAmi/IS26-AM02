package it.polimi.ingsw.am02.common.enumerations;

/**
 * Enumerates every distinct phase in the game FSM.
 * Phase transitions are broadcast via
 * {@link it.polimi.ingsw.am02.common.interfaces.VirtualView#notifyPhaseChanged}
 * and observed by building effects registered as
 * {@link it.polimi.ingsw.am02.server.model.listeners.PhaseObserver}.
 *
 * <ul>
 *   <li>{@link #SETUP}                  — board initialization, first turn order established</li>
 *   <li>{@link #TOTEM_PLACEMENT}        — players place their totems on offer tiles in order</li>
 *   <li>{@link #ACTION_RESOLUTION}      — current player selects cards or returns totem</li>
 *   <li>{@link #END_PLAYER_TURN}        — turn-order rewards applied; {@code TurnOrderFoodBonusEffect} fires here</li>
 *   <li>{@link #END_ROUND}              — all players have acted; extra turns enqueued; {@code ExtraTurnEffect} fires here</li>
 *   <li>{@link #EVENT_RESOLUTION}       — non-final event cards in the lower row are resolved</li>
 *   <li>{@link #NEW_ROUND}              — board rows refreshed for the next round</li>
 *   <li>{@link #NEW_ERA}                — tribe deck era changed; building market rows updated</li>
 *   <li>{@link #FINAL_EVENT_RESOLUTION} — final event cards resolved at game end</li>
 *   <li>{@link #END_GAME}               — final scoring; end-game building effects fire here</li>
 * </ul>
 */
public enum PhaseType {
    SETUP, TOTEM_PLACEMENT, ACTION_RESOLUTION, END_PLAYER_TURN, END_ROUND, EVENT_RESOLUTION , NEW_ROUND, NEW_ERA, FINAL_EVENT_RESOLUTION, END_GAME
}
