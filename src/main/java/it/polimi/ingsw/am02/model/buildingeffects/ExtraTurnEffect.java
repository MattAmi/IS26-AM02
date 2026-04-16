package it.polimi.ingsw.am02.model.buildingeffects;

import it.polimi.ingsw.am02.model.*;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;

public class ExtraTurnEffect implements BuildingEffect, PhaseObserver {

    private final Player player;
    private final Game game;

    private final int extraUpperPicks;
    private final int extraLowerPicks;

    public ExtraTurnEffect(Player player, Game game, int extraUpperPicks, int extraLowerPicks) {
        this.player = player;
        this.game = game;
        this.extraUpperPicks = extraUpperPicks;
        this.extraLowerPicks = extraLowerPicks;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    @Override
    public void onPhaseChange(PhaseType newPhase) {
        if (newPhase == PhaseType.END_ROUND) {

            String nickname = player.getNickname();
            game.enqueueExtraTurn(nickname, extraUpperPicks, extraLowerPicks);

        }
    }
}