package it.polimi.ingsw.am02.common.messages;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public sealed interface Message extends Serializable
        permits Command, Event {
}
