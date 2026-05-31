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

/**
 * Building effect that rewards food whenever the owner's tribe accumulates a new pair
 * of Inventors sharing the same {@link InventionType}.
 *
 * <p>Registered as a {@link TribuObserver}. Only fires when an {@code INVENTOR} character
 * is inserted. Uses a counter ({@code pairsAlreadyRewarded}) to track already-rewarded pairs,
 * so each new pair is rewarded exactly once.
 *
 * <p>JSON key: {@code "INVENTOR_PAIR_FOOD_REWARD"}
 */
public class InventorPairRewardEffect implements BuildingEffect, TribuObserver {
    private final Player owner;
    private final int foodReward;
    private int pairsAlreadyRewarded;

    /**
     * @param owner      the player who owns this building
     * @param foodReward food points granted per newly completed Inventor pair
     */
    public InventorPairRewardEffect(Player owner, int foodReward) {
        this.owner = owner;
        this.foodReward = foodReward;
        this.pairsAlreadyRewarded = calculateCurrentPairs(owner.getTribu());
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor v) {
        v.visitTribuObserver(this);
    }

    /**
     * After an Inventor character is inserted, checks whether any new same-type
     * Inventor pairs have been formed and grants the food reward for each.
     * Has no effect when a non-Inventor character is inserted.
     *
     * @param type the type of character just inserted
     * @return an {@link EffectOutcome} with the food delta for all new pairs, or empty
     */
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