package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;

public class InventorPairRewardEffect implements BuildingEffect, TribuObserver {

    private final Tribu tribu;
    private final int foodReward; // Dal regolamento, sarà 3
    private int pairsAlreadyRewarded;

    public InventorPairRewardEffect(Tribu tribu, int foodReward) {
        this.tribu = tribu;
        this.foodReward = foodReward;
        this.pairsAlreadyRewarded = calculateCurrentPairs();
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitTribuObserver(this); // Corretto: mancava il 'this'
    }

    @Override
    public void onCharacterInsertion(CharacterType type) {
        if(type == CharacterType.INVENTOR) {
            int currentPairs = calculateCurrentPairs();
            if (currentPairs > pairsAlreadyRewarded) {
                int newPairsFormed = currentPairs - pairsAlreadyRewarded;

                tribu.addFoodPoints(foodReward * newPairsFormed);

                pairsAlreadyRewarded = currentPairs;

            }
        }
    }

    private int calculateCurrentPairs() {
        int totalPairs = 0;
        for (InventionType type : InventionType.values()) {
            int count = tribu.getInventionTypeCount(type);
            totalPairs += (count / 2);
        }
        return totalPairs;
    }
}