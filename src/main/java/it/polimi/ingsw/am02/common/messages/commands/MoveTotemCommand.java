package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.interfaces.VirtualControllerManager;

/**
 * In-game command requesting that the current player move their totem
 * to an offer tile or return it to the turn-order tile.
 *
 * @param nickname the player the command is attributed to; populated
 *                 server-side and sent blank by the client — see
 *                 {@link GameCommand} for why it is not used for routing
 * @param tileID   the single-character identifier of the target tile
 *                 (use {@code 'T'} to return the totem)
 */
public record MoveTotemCommand(String nickname, char tileID) implements GameCommand {
    @Override
    public void apply(VirtualControllerManager manager, String clientId) {
        manager.requestMoveTotem(clientId, tileID);
    }
}