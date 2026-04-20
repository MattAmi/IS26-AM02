package it.polimi.ingsw.am02.common.messages;

import java.io.Serializable;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;


public sealed interface Message extends Serializable
        permits Command, Event {
}
