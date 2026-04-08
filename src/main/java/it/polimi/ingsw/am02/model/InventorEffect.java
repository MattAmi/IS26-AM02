package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.InventionType;

public class InventorEffect implements CharacterEffect {

    private final InventionType invention;

    //Costruttore
    public InventorEffect(InventionType invention) {
        this.invention = invention;
    }

    //Inventor Effect: adds a new inventor/inventionType to the tribu
    @Override
    public void applyEffect(Tribu tribu) {
        tribu.addInventionType(invention);
    }

}

