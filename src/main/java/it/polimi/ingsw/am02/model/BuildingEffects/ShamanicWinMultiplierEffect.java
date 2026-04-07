package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.*;
import it.polimi.ingsw.am02.model.Enumerations.EventType;

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
    public void EventStart(EventType currentEvent) {
        // Qui non facciamo nulla, l'effetto scatta alla risoluzione
    }

    @Override
    public void EventResolution(EventType currentEvent, EventCard event) {
        if (currentEvent == EventType.SHAMANIC_RITUAL) {

            int maxStars = game.calculateMaxShamanStars();
            int myShamanStars = tribu.getShamanStars();

            if (myShamanStars >= maxStars && maxStars > 0) {

                int baseBonusPP = event.getMajorityBonus();
                int extraBonus = baseBonusPP * (multiplier - 1);

                tribu.addPrestigePoints(extraBonus);
            }
        }
    }
}