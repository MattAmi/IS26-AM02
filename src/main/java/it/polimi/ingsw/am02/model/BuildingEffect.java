package it.polimi.ingsw.am02.model;

public interface BuildingEffect {
    void accept(EffectVisitor v);

}
