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

/**
 * Building effect that rewards food whenever the owner's tribe completes a new full set
 * of all character types (i.e. gains at least one character of every {@link CharacterType}).
 *
 * <p>Registered as a {@link TribuObserver}. Fires after each character insertion.
 * Uses a counter ({@code setsAlreadyRewarded}) to track how many complete sets
 * have already been rewarded, so only newly completed sets trigger a payout.
 *
 * <p>JSON key: {@code "FULL_SET_FOOD_REWARD"}
 */
public class FullSetFoodRewardEffect implements BuildingEffect, TribuObserver {
    private final Player owner;
    private final int foodBonus;
    private int setsAlreadyRewarded;

    /**
     * @param owner      the player who owns this building
     * @param foodReward food points granted each time a new complete character set is formed
     */
    public FullSetFoodRewardEffect(Player owner, int foodReward) {
        this.owner = owner;
        this.foodBonus = foodReward;
        this.setsAlreadyRewarded = calculateCurrentSets(owner.getTribu());
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitTribuObserver(this);
    }

    /**
     * Checks whether a new complete set has been formed after the most recent character insertion.
     * If so, grants the food reward and updates the rewarded-set counter.
     *
     * @param type the type of character just inserted (used to re-evaluate the set count)
     * @return an {@link EffectOutcome} with the food delta if a new set was completed, or empty
     */
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