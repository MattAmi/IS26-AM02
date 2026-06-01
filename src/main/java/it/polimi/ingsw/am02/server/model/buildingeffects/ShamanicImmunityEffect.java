package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.effect.BuildingEffect;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;

/**
 * Building effect that makes the owner's tribe immune to the minority penalty
 * during the Shamanic Ritual event.
 *
 * <p>Registered as an {@link EventObserver}. Sets the immunity flag on the tribe
 * in {@link #eventStart} and clears it in {@link #eventEnd}.
 *
 * <p>JSON key: {@code "SHAMANIC_IMMUNITY"}
 */
public class ShamanicImmunityEffect implements BuildingEffect, EventObserver {
    final Player owner;

    /**
     * @param owner the player who owns this building
     */
    public ShamanicImmunityEffect(Player owner) {
        this.owner = owner;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor v) {
        v.visitEventObserver(this);
    }

    /**
     * Grants immunity to the owner's tribe at the start of the Shamanic Ritual.
     * Has no effect for other event types.
     *
     * @param eventType the event currently starting
     * @return an empty {@link EffectOutcome} (immunity is a flag, not a resource delta)
     */
    @Override
    public EffectOutcome eventStart(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL) {
            owner.getTribu().setImmuneToShamanicPenalty(true);
        }

        return EffectOutcome.empty();
    }

    /**
     * Revokes immunity from the owner's tribe after the Shamanic Ritual resolves.
     * Has no effect for other event types.
     *
     * @param eventType the event that just finished
     * @return an empty {@link EffectOutcome}
     */
    @Override
    public EffectOutcome eventEnd(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL) {
            owner.getTribu().setImmuneToShamanicPenalty(false);
        }

        return EffectOutcome.empty();
    }

}
