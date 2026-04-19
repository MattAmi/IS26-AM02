package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.Tribu;

public class ShamanicImmunityEffect implements BuildingEffect, EventObserver {
    final Player owner;

    public ShamanicImmunityEffect(Player owner) {
        this.owner = owner;
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitEventObserver(this);
    }

    @Override
    public EffectOutcome eventStart(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL) {
            owner.getTribu().setImmuneToShamanicPenalty(true);
        }

        return EffectOutcome.empty();
    }

    @Override
    public EffectOutcome eventEnd(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL) {
            owner.getTribu().setImmuneToShamanicPenalty(false);
        }

        return EffectOutcome.empty();
    }

}
