package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;

/**
 * Concrete {@link EffectVisitor} that registers building effects with the
 * appropriate observer lists in the current {@link Game} and {@link Tribu}.
 *
 * <p>Called once per building acquisition via {@link Tribu#insertBuilding}.
 */
public class RegistrationVisitor implements EffectVisitor {

    final Game game;
    final Tribu tribu;

    /**
     * @param game  the current game, used to register phase and event observers
     * @param tribu the tribe of the player who acquired the building, used to register tribe observers
     */
    public RegistrationVisitor(Game game, Tribu tribu) {
        this.game = game;
        this.tribu = tribu;
    }

    /** {@inheritDoc} */
    @Override
    public void visitPhaseObserver(PhaseObserver effect) {
        game.attachPhaseObserver(effect);
    }

    /** {@inheritDoc} */
    @Override
    public void visitTribuObserver(TribuObserver effect) {
        tribu.attachTribuObserver(effect);
    }

    /** {@inheritDoc} */
    @Override
    public void visitEventObserver(EventObserver effect){
        game.getGameBoard().attachEventObserver(effect);
    }

}