package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Tribu;
import it.polimi.ingsw.am02.model.TribuObserver;

public class FullSetFoodRewardEffect implements BuildingEffect, TribuObserver {

    private final Tribu tribu;
    private final int foodBonus;

    private int setsAlreadyRewarded;

    public FullSetFoodRewardEffect(Tribu tribu, int foodReward) {
        this.tribu = tribu;
        this.foodBonus = foodReward;
        this.setsAlreadyRewarded = calculateCurrentSets();
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitTribuObserver(this);
    }

    @Override
    public void onCharacterInsertion(CharacterType type) {
        int currentSets = calculateCurrentSets();

        if (currentSets > setsAlreadyRewarded) {
            tribu.addFoodPoints(foodBonus);
            setsAlreadyRewarded = currentSets;
        }
    }

    private int calculateCurrentSets() {
        int minPezzi = Integer.MAX_VALUE;

        for (CharacterType type : CharacterType.values()) {
            int count = tribu.getCharacterCount(type);
            if (count < minPezzi) {
                minPezzi = count;
            }
        }
        return minPezzi;
    }

}