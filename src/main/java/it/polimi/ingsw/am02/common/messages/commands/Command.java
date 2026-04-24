package it.polimi.ingsw.am02.common.messages.commands;

import it.polimi.ingsw.am02.common.messages.Message;


public sealed interface Command extends Message permits GameCommand, LobbyCommand, ReconnectCommand {}

