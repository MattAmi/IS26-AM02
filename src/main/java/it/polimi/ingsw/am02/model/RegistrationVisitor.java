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
    //TODO Husnain: non è metodo statico

    @Override
    public void visitTribuObserver(TribuObserver effect) {
        Tribu.attachTribuObserver(effect);
    }
    //TODO Husnain: non è metodo statico

    @Override
    public void visitEventObserver(EventObserver effect){
        EventCard.attachEventObserver(effect);
    }
    //TODO Husnain: non è metodo statico

}