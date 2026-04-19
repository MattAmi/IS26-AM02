package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;

public interface EventObserver {
    default EffectOutcome EventStart(EventType eventType) { return EffectOutcome.empty(); }
    default EffectOutcome EventPostResolution(EventType eventType) { return EffectOutcome.empty(); }
    default EffectOutcome EventEnd(EventType eventType) { return EffectOutcome.empty(); }
}
