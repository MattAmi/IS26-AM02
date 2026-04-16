package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.*;
import it.polimi.ingsw.am02.model.enumerations.EventType;

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
