package it.polimi.ingsw.am02.model;

public interface BuildingEffect {
    public void accept(EffectVisitor v);
}
