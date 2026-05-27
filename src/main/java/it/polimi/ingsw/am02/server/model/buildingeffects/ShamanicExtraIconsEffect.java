package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;

import java.util.List;

/**
 * Building effect that temporarily adds bonus shaman stars to the owner's tribe
 * for the duration of the Shamanic Ritual event.
 *
 * <p>Registered as an {@link EventObserver}. Stars are added in {@link #eventStart}
 * and removed in {@link #eventEnd}, keeping the tribe's permanent star count unchanged.
 *
 * <p>JSON key: {@code "EXTRA_SHAMAN_STARS"}
 */
public class ShamanicExtraIconsEffect implements BuildingEffect, EventObserver {
    private final Player owner;
    private final int bonusStars;

    /**
     * @param owner      the player who owns this building
     * @param bonusStars the number of extra shaman stars added during the Shamanic Ritual
     */
    public ShamanicExtraIconsEffect(Player owner, int bonusStars) {
        this.owner = owner;
        this.bonusStars = bonusStars;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

    /**
     * Adds the bonus shaman stars at the start of the Shamanic Ritual.
     * Has no effect for other event types.
     *
     * @param eventType the event currently starting
     * @return an {@link EffectOutcome} with the shaman-stars delta, or empty
     */
    @Override
    public EffectOutcome eventStart(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();


            tribu.addShamanStars(bonusStars);

            return new EffectOutcome(List.of(
                    new ResourceDelta(nickname, ResourceType.SHAMAN_STARS, tribu.getShamanStars(), bonusStars)
            ));
        }

        return EffectOutcome.empty();
    }

    /**
     * Removes the bonus shaman stars after the Shamanic Ritual resolves.
     * Has no effect for other event types.
     *
     * @param eventType the event that just finished
     * @return an {@link EffectOutcome} with the negative shaman-stars delta, or empty
     */
    @Override
    public EffectOutcome eventEnd(EventType eventType) {
        if(eventType == EventType.SHAMANIC_RITUAL) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            tribu.addShamanStars(-bonusStars);

            return new EffectOutcome(List.of(
                    new ResourceDelta(nickname, ResourceType.SHAMAN_STARS, tribu.getShamanStars(), -bonusStars)
            ));
        }

        return EffectOutcome.empty();
    }
}
