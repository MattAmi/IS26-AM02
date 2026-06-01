package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.effect.BuildingEffect;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

/**
 * Building effect that grants the owner an extra turn at the end of each round.
 *
 * <p>Registered as a {@link PhaseObserver}. When the {@link PhaseType#END_ROUND} phase
 * is entered, this effect calls {@link Game#enqueueExtraTurn} to schedule an additional
 * action-resolution turn for the owner before the next round begins.
 *
 * <p>JSON key: {@code "EXTRA_TURN"}
 */
public class ExtraTurnEffect implements BuildingEffect, PhaseObserver {
    private final Player owner;
    private final Game game;

    private final int extraUpperPicks;
    private final int extraLowerPicks;

    /**
     * @param owner           the player who owns this building
     * @param game            the current game instance, used to enqueue the extra turn
     * @param extraUpperPicks the number of upper-row picks allowed during the extra turn
     * @param extraLowerPicks the number of lower-row picks allowed during the extra turn
     */
    public ExtraTurnEffect(Player owner, Game game, int extraUpperPicks, int extraLowerPicks) {
        this.owner = owner;
        this.game = game;
        this.extraUpperPicks = extraUpperPicks;
        this.extraLowerPicks = extraLowerPicks;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    /**
     * Enqueues an extra turn for the owner when the end-of-round phase is entered.
     * Has no effect during any other phase transition.
     *
     * @param newPhase the phase the game is transitioning into
     * @return an empty {@link EffectOutcome} (the extra turn produces no immediate resource delta)
     */
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