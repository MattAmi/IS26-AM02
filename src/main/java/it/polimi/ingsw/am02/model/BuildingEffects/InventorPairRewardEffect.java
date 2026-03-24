package it.polimi.ingsw.am02.model.BuildingEffects;

import it.polimi.ingsw.am02.model.BuildingEffect;
import it.polimi.ingsw.am02.model.CharacterCard;
import it.polimi.ingsw.am02.model.EffectVisitor;
import it.polimi.ingsw.am02.model.TribuObserver;

public class InventorPairRewardEffect implements BuildingEffect, TribuObserver {


    @Override
    public void accept(EffectVisitor v) {

    }

    @Override
    public void onCharacterInsertion(CharacterCard newCharacter) {

    }
}
