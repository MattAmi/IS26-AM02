package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.client.model.GameModel;

/**
 * Broadcast to all players (except the disconnected one) immediately
 * before AutoPlayer executes each individual command on behalf of a
 * disconnected player.
 *
 * @param nickname the disconnected player being substituted
 */
public record AutoPlayerInvokedEvent(String nickname) implements GameEvent {
    @Override public void applyTo(GameModel model) { model.updateAutoPlayerInvoked(nickname); }
}