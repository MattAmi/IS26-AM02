package it.polimi.ingsw.am02.server.controller.persistence;

import it.polimi.ingsw.am02.common.messages.commands.GameCommand;

public record CommandRecord(GameCommand command, String nickname) {}

