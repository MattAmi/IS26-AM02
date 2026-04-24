package it.polimi.ingsw.am02.common.interfaces;

import it.polimi.ingsw.am02.common.dto.*;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.messages.events.Event;

import java.util.List;
import java.util.Map;

public interface VirtualView {
    void notify(Event event);


    static VirtualView noOp() {
        return event -> {};
    }
}
