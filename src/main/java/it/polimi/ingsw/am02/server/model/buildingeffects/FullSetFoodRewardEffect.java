package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;

import java.util.List;

public class FullSetFoodRewardEffect implements BuildingEffect, TribuObserver {
    private final Player owner;
    private final int foodBonus;
    private int setsAlreadyRewarded;

    public FullSetFoodRewardEffect(Player owner, int foodReward) {
        this.owner = owner;
        this.foodBonus = foodReward;
        this.setsAlreadyRewarded = calculateCurrentSets(owner.getTribu());
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitTribuObserver(this);
    }

    @Override
    public EffectOutcome onCharacterInsertion(CharacterType type) {
        Tribu tribu = owner.getTribu();
        String nickname = owner.getNickname();

        int currentSets = calculateCurrentSets(tribu);

        if (currentSets > setsAlreadyRewarded) {
            tribu.addFoodPoints(foodBonus);
            setsAlreadyRewarded = currentSets;

            return new EffectOutcome(List.of(
                    new ResourceDelta(nickname, ResourceType.FOOD, tribu.getFoodPoints(), foodBonus)
            ));
        }

        return EffectOutcome.empty();
    }

    private int calculateCurrentSets(Tribu tribu) {
        int minPezzi = Integer.MAX_VALUE;
        for (CharacterType type : CharacterType.values()) {
            int count = tribu.getCharacterCount(type);
            if (count < minPezzi)
                minPezzi = count;
        }

        return minPezzi;
    }

}