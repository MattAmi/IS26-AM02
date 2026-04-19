package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

public class ExtraTurnEffect implements BuildingEffect, PhaseObserver {
    private final Player owner;
    private final Game game;

    private final int extraUpperPicks;
    private final int extraLowerPicks;

    public ExtraTurnEffect(Player owner, Game game, int extraUpperPicks, int extraLowerPicks) {
        this.owner = owner;
        this.game = game;
        this.extraUpperPicks = extraUpperPicks;
        this.extraLowerPicks = extraLowerPicks;
    }

    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if (newPhase == PhaseType.END_ROUND) {

            String nickname = owner.getNickname();
            game.enqueueExtraTurn(nickname, extraUpperPicks, extraLowerPicks);

            return EffectOutcome.empty();
        }
        return EffectOutcome.empty();
    }
}