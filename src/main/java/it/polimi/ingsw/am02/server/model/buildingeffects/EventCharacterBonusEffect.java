package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.Tribu;

import java.util.ArrayList;
import java.util.List;

public class EventCharacterBonusEffect implements BuildingEffect, EventObserver {
    private final Player owner;
    private final EventType eventType;
    private final CharacterType targetCharacter;
    private final int foodReward;
    private final int prestigeReward;
    private final int foodDiscount;

    public EventCharacterBonusEffect(Player owner, EventType eventType, CharacterType targetCharacter, int foodReward, int prestigeReward, int foodDiscount) {
        this.owner = owner;
        this.eventType = eventType;
        this.targetCharacter = targetCharacter;
        this.foodReward = foodReward;
        this.prestigeReward = prestigeReward;
        this.foodDiscount = foodDiscount;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

    @Override
    public EffectOutcome eventStart(EventType eventType) {
        if(eventType == this.eventType) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int n = tribu.getCharacterCount(targetCharacter);

            if (n > 0) {
                List<ResourceDelta> deltas = new ArrayList<>();

                int totalFood = foodReward * n;
                int totalPP = prestigeReward * n;
                int totalDiscount = foodDiscount * n;

                if (totalFood > 0) {
                    tribu.addFoodPoints(totalFood);
                    deltas.add(new ResourceDelta(nickname, ResourceType.FOOD, tribu.getFoodPoints(), totalFood));
                }

                if (totalPP > 0) {
                    tribu.addPrestigePoints(totalPP);
                    deltas.add(new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), totalPP));
                }

                if (totalDiscount > 0) {
                    tribu.addFoodDiscount(totalDiscount);
                    deltas.add(new ResourceDelta(nickname, ResourceType.FOOD_DISCOUNT, tribu.getFoodDiscount(), totalDiscount));
                }

                return new EffectOutcome(deltas);
            }
        }
        return EffectOutcome.empty();
    }

    @Override
    public EffectOutcome eventEnd(EventType eventType) {
        if(eventType == this.eventType) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int n = tribu.getCharacterCount(targetCharacter);

            if (n > 0 && foodDiscount > 0) {
                int totalDiscount = foodDiscount * n;
                tribu.addFoodDiscount(-totalDiscount);

                return new EffectOutcome(List.of(
                        new ResourceDelta(nickname, ResourceType.FOOD_DISCOUNT, tribu.getFoodDiscount(), -totalDiscount)
                ));
            }
        }
        return EffectOutcome.empty();
    }

}
