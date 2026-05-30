package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.messages.commands.GameCommand;

/**
 * Pairs a deserialized {@link it.polimi.ingsw.am02.common.messages.commands.GameCommand}
 * with the nickname of the player who issued it, as stored in the NDJSON log.
 *
 * @param command  the game command to replay
 * @param nickname the player who originally issued this command
 */
public record CommandRecord(GameCommand command, String nickname) {}

