package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;

import java.util.List;

public class InventorPairRewardEffect implements BuildingEffect, TribuObserver {
    private final Player owner;
    private final int foodReward;
    private int pairsAlreadyRewarded;

    public InventorPairRewardEffect(Player owner, int foodReward) {
        this.owner = owner;
        this.foodReward = foodReward;
        this.pairsAlreadyRewarded = calculateCurrentPairs(owner.getTribu());
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitTribuObserver(this);
    }

    @Override
    public EffectOutcome onCharacterInsertion(CharacterType type) {
        if(type == CharacterType.INVENTOR) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int currentPairs = calculateCurrentPairs(tribu);

            if (currentPairs > pairsAlreadyRewarded) {
                int newPairsFormed = currentPairs - pairsAlreadyRewarded;
                int totalBonus = foodReward * newPairsFormed;

                tribu.addFoodPoints(totalBonus);
                pairsAlreadyRewarded = currentPairs;

                return new EffectOutcome(List.of(
                        new ResourceDelta(nickname, ResourceType.FOOD, tribu.getFoodPoints(), totalBonus)
                ));
            }
        }

        return EffectOutcome.empty();
    }

    private int calculateCurrentPairs(Tribu tribu) {
        int totalPairs = 0;
        for (InventionType invType : InventionType.values()) {
            int count = tribu.getInventionTypeCount(invType);
            totalPairs += (count / 2);
        }
        return totalPairs;
    }
}