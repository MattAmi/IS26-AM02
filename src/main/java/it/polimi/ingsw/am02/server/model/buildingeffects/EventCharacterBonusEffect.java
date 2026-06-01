package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.effect.BuildingEffect;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.player.Tribu;

import java.util.ArrayList;
import java.util.List;

/**
 * Building effect that grants food, prestige points, and/or a temporary food discount
 * at the start of a specific event, scaled by the number of a target character type
 * in the owner's tribe.
 *
 * <p>Registered as an {@link EventObserver}. The food discount granted in
 * {@link #eventStart} is automatically reversed in {@link #eventEnd} to keep it
 * scoped to the duration of the event resolution.
 *
 * <p>JSON key: {@code "EVENT_CHARACTER_BONUS"}
 */
public class EventCharacterBonusEffect implements BuildingEffect, EventObserver {
    private final Player owner;
    private final EventType eventType;
    private final CharacterType targetCharacter;
    private final int foodReward;
    private final int prestigeReward;
    private final int foodDiscount;

    /**
     * @param owner           the player who owns this building
     * @param eventType       the specific event type that triggers this bonus
     * @param targetCharacter the character type whose count scales the rewards
     * @param foodReward      food points granted per character of the target type
     * @param prestigeReward  prestige points granted per character of the target type
     * @param foodDiscount    food discount granted per character of the target type (reversed after the event)
     */
    public EventCharacterBonusEffect(Player owner, EventType eventType, CharacterType targetCharacter, int foodReward, int prestigeReward, int foodDiscount) {
        this.owner = owner;
        this.eventType = eventType;
        this.targetCharacter = targetCharacter;
        this.foodReward = foodReward;
        this.prestigeReward = prestigeReward;
        this.foodDiscount = foodDiscount;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

    /**
     * Fires at the start of the matching event.
     * Grants food, prestige points, and a temporary food discount
     * proportional to {@code targetCharacter} count. Has no effect for other event types.
     *
     * @param eventType the event currently starting
     * @return an {@link EffectOutcome} with all resource deltas, or empty if not the target event
     *         or if the owner has no characters of the target type
     */
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

    /**
     * Fires at the end of the matching event to reverse the temporary food discount.
     * Has no effect for other event types or if no discount was applied.
     *
     * @param eventType the event that just finished resolving
     * @return an {@link EffectOutcome} with the reversed food-discount delta, or empty
     */
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
