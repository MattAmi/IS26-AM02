
package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.*;
import it.polimi.ingsw.am02.model.Enumerations.PhaseType;

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
        if(newPhase == PhaseType.END_PLAYER_TURN && Game.validatePlayerTurn(player)){
            Game.getGameBoard().getTurnOrderTile().applyRewards(player);
        }
    }
}
