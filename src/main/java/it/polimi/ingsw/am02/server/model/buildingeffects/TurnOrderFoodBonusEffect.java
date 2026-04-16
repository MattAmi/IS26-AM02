
package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

public class TurnOrderFoodBonusEffect implements BuildingEffect, PhaseObserver {

    final Player player;
    final Game game;

    public TurnOrderFoodBonusEffect(Player player, Game game) {
        this.player = player;
        this.game = game;
    }

    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    @Override
    public void onPhaseChange(PhaseType newPhase) {
        if (newPhase == PhaseType.END_PLAYER_TURN) {
            game.triggerTurnOrderExtraFood(player.getTribu());
        }
    }
}
