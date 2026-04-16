package it.polimi.ingsw.am02.server.model;

public interface BuildingEffect {
    void accept(EffectVisitor v);

}
