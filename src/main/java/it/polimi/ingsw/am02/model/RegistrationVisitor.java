package it.polimi.ingsw.am02.model;

public class RegistrationVisitor implements EffectVisitor {

    final Game game;
    final Tribu tribu;

    public RegistrationVisitor(Game game, Tribu tribu) {
        this.game = game;
        this.tribu = tribu;
    }

    @Override
    public void visitPhaseObserver(PhaseObserver effect) {
        Game.attachPhaseObserver(effect);
    }

    @Override
    public void visitTribuObserver(TribuObserver effect) {
        Tribu.attachTribuObserver(effect);
    }
}