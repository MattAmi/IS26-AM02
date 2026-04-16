package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.server.model.listeners.EventObserver;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;

public class RegistrationVisitor implements EffectVisitor {

    final Game game;
    final Tribu tribu;

    public RegistrationVisitor(Game game, Tribu tribu) {
        this.game = game;
        this.tribu = tribu;
    }

    @Override
    public void visitPhaseObserver(PhaseObserver effect) {
        game.attachPhaseObserver(effect);
    }

    @Override
    public void visitTribuObserver(TribuObserver effect) {
        tribu.attachTribuObserver(effect);
    }

    @Override
    public void visitEventObserver(EventObserver effect){
        game.getGameBoard().attachEventObserver(effect);
    }

}