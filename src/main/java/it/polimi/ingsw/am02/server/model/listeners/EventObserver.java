package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;

public interface EventObserver {
    default EffectOutcome eventStart(EventType eventType) { return EffectOutcome.empty(); }
    default EffectOutcome eventPostResolution(EventType eventType) { return EffectOutcome.empty(); }
    default EffectOutcome eventEnd(EventType eventType) { return EffectOutcome.empty(); }
}
