package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.CharacterType;

public interface TribuObserver {
    void onCharacterInsertion(CharacterType newCharacter);
}