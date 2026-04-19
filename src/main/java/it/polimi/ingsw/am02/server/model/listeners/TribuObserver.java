package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;

public interface TribuObserver {
    EffectOutcome onCharacterInsertion(CharacterType newCharacter);
}