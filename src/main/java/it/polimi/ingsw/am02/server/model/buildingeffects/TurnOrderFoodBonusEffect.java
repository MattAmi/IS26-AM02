
package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.List;

public class TurnOrderFoodBonusEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final Game game;

    public TurnOrderFoodBonusEffect(Player owner, Game game) {
        this.owner = owner;
        this.game = game;
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if (newPhase == PhaseType.END_PLAYER_TURN) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int foodBefore = tribu.getFoodPoints();

            game.triggerTurnOrderExtraFood(tribu);

            int foodAfter = tribu.getFoodPoints();
            int foodGained = foodAfter - foodBefore;

            if (foodGained > 0) {
                return new EffectOutcome(List.of(
                        new ResourceDelta(nickname, ResourceType.FOOD, foodAfter, foodGained)
                ));
            }
        }
        return EffectOutcome.empty();
    }
}
