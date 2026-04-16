package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.Tribu;

public class ShamanicImmunityEffect implements BuildingEffect, EventObserver {

    final Tribu tribu;

    public ShamanicImmunityEffect(Tribu tribu) {
        this.tribu = tribu;
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitEventObserver(this);
    }

    @Override
    public void EventStart(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL){
            tribu.setImmuneToShamanicPenalty(true);
        }
    }

    public void EventEnd(EventType eventType){
        if(eventType == EventType.SHAMANIC_RITUAL){
            tribu.setImmuneToShamanicPenalty(false);
        }
    }

}
