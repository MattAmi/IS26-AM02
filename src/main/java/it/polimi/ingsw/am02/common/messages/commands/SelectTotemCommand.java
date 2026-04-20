package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.enumerations.Totem;

public record SelectTotemCommand(Totem color) implements LobbyCommand {}