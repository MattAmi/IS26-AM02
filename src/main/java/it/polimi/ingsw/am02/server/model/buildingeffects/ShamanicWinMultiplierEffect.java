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

public class ShamanicWinMultiplierEffect implements BuildingEffect, EventObserver {
    private final Player owner;
    private final int multiplier;

    public ShamanicWinMultiplierEffect(Player owner, int multiplier) {
        this.owner = owner;
        this.multiplier = multiplier;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

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