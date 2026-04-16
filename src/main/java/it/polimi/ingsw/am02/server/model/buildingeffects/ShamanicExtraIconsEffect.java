package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;

public class ShamanicExtraIconsEffect implements BuildingEffect, EventObserver {

    final Tribu tribu;
    final int bonusStars;

    public ShamanicExtraIconsEffect(Tribu tribu, int bonusStars) {
        this.tribu = tribu;
        this.bonusStars = bonusStars;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

    @Override
    public void EventStart(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL){
            tribu.addShamanStars(bonusStars);
        }
    }
    public void EventEnd(EventType evenType) {
        if(evenType == EventType.SHAMANIC_RITUAL){
            tribu.addShamanStars(-bonusStars);
        }
    }
}
