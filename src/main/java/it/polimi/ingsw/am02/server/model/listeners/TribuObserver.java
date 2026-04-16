package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.model.enumerations.CharacterType;

public interface TribuObserver {
    void onCharacterInsertion(CharacterType newCharacter);

}