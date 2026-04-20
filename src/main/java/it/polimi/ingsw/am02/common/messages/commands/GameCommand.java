package it.polimi.ingsw.am02.common.messages.commands;


public sealed interface GameCommand extends Command
        permits MoveTotemCommand, ResolveActionsCommand {}
