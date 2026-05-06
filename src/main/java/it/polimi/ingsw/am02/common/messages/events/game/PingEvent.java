package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record PingEvent() implements Event {
    @Override
    public void apply(VirtualView view) {}
}
