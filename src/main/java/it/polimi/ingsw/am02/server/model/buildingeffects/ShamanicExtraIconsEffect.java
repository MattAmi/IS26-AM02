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

public class ShamanicExtraIconsEffect implements BuildingEffect, EventObserver {
    private final Player owner;
    private final int bonusStars;

    public ShamanicExtraIconsEffect(Player owner, int bonusStars) {
        this.owner = owner;
        this.bonusStars = bonusStars;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

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
