package it.polimi.ingsw.am02.server.controller.persistence;

/**
 * Tracks a player's connection lifecycle within a single game session.
 * Used by {@link it.polimi.ingsw.am02.server.controller.GameController}
 * to determine whether to forward notifications, arm timers, or invoke
 * Autoplayer.
 *
 * <ul>
 *   <li>{@link #CONNECTED}            — the player is actively connected and receiving events</li>
 *   <li>{@link #DISCONNECTED}         — the player lost their connection during a live game;
 *                                       AutoPlayer may act for them after the grace period expires</li>
 *   <li>{@link #RECONNECTING}         — the player has reconnected and is receiving the catch-up
 *                                       event history (drain in progress); treated as active for
 *                                       timer purposes</li>
 *   <li>{@link #PENDING_RECONNECTION} — set during server-side crash recovery; the player has not
 *                                       yet reconnected to the recovered game session</li>
 * </ul>
 */
public enum ConnectionStatus {
    CONNECTED,  DISCONNECTED, RECONNECTING, PENDING_RECONNECTION
}
