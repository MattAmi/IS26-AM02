package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;

import java.util.List;

/**
 * Building effect that multiplies the prestige bonus the owner received for winning
 * the Shamanic Ritual event.
 *
 * <p>Registered as an {@link EventObserver}. Fires in {@link #eventPostResolution}:
 * reads the bonus the owner received from {@link Tribu#getLastEventBonusReceived()}
 * and adds {@code bonus × (multiplier - 1)} extra prestige points.
 * Only activates if the owner actually received a positive bonus (i.e. was the majority winner).
 *
 * <p>JSON key: {@code "SHAMANIC_WIN_MULTIPLIER"}
 */
public class ShamanicWinMultiplierEffect implements BuildingEffect, EventObserver {
    private final Player owner;
    private final int multiplier;

    /**
     * @param owner      the player who owns this building
     * @param multiplier the factor applied to the Shamanic Ritual majority bonus
     */
    public ShamanicWinMultiplierEffect(Player owner, int multiplier) {
        this.owner = owner;
        this.multiplier = multiplier;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

    /**
     * After the Shamanic Ritual resolves, multiplies the owner's majority bonus.
     * The extra prestige added is {@code lastBonus × (multiplier - 1)}.
     * Has no effect for other event types or if the owner received no bonus.
     *
     * @param currentEvent the event that just finished resolving
     * @return an {@link EffectOutcome} with the extra prestige-point delta, or empty
     */
    @Override
    public EffectOutcome eventPostResolution(EventType currentEvent) {
        if (currentEvent == EventType.SHAMANIC_RITUAL) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int bonusReceived = tribu.getLastEventBonusReceived();

            if (bonusReceived > 0) {
                int extraBonus = bonusReceived * (multiplier - 1);
                tribu.addPrestigePoints(extraBonus);

                return new EffectOutcome(List.of(
                        new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), extraBonus)
                ));
            }
        }

        return EffectOutcome.empty();
    }
}