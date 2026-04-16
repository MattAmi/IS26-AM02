package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.listeners.EventObserver;

public class ShamanicWinMultiplierEffect implements BuildingEffect, EventObserver {

    private final Tribu tribu;
    private final Game game;
    private final int multiplier;

    public ShamanicWinMultiplierEffect(Tribu tribu, Game game, int multiplier) {
        this.tribu = tribu;
        this.game = game;
        this.multiplier = multiplier;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitEventObserver(this);
    }

    @Override
    public void EventPostResolution(EventType currentEvent) {
        if (currentEvent == EventType.SHAMANIC_RITUAL) {
            int bonusReceived = tribu.getLastEventBonusReceived();
            if (bonusReceived > 0) {
                int extraBonus = bonusReceived * (multiplier - 1);
                tribu.addPrestigePoints(extraBonus);
            }
        }
    }
}