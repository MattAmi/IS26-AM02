package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.model.enumerations.EventType;
import it.polimi.ingsw.am02.model.EventObserver;
import it.polimi.ingsw.am02.model.Tribu;

public class EventCharacterBonusEffect implements BuildingEffect, EventObserver {
    final EventType eventType;
    final CharacterType targetCharacter;
    final int foodReward;
    final int prestigeReward;
    final int foodDiscount;
    final Tribu tribu;

    public EventCharacterBonusEffect(EventType eventType, CharacterType targetCharacter, int foodReward, int prestigeReward, int foodDiscount, Tribu tribu) {
        this.eventType = eventType;
        this.targetCharacter = targetCharacter;
        this.foodReward = foodReward;
        this.prestigeReward = prestigeReward;
        this.foodDiscount = foodDiscount;
        this.tribu = tribu;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }


    @Override
    public void EventStart(EventType eventType) {
        if(eventType == this.eventType) {
            int n = tribu.getCharacterCount(targetCharacter);
            tribu.addFoodPoints(foodReward * n);
            tribu.addPrestigePoints(prestigeReward * n);
            tribu.addFoodDiscount(foodDiscount * n);
        }
    }

    @Override
    public void EventEnd(EventType eventType){
        if(eventType == this.eventType){
            tribu.addFoodDiscount(-foodDiscount);
        }
    }

}
