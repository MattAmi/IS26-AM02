package it.polimi.ingsw.am02.server.model.listeners;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;

/**
 * Observer notified whenever a character card is inserted into a tribe.
 * Implemented by building effects that react to character acquisitions
 * (e.g. full-set bonuses, Inventor-pair rewards).
 */
public interface TribuObserver {

    /**
     * Called immediately after a character of the given type is added to the tribe.
     *
     * @param newCharacter the type of character that was just inserted
     * @return an {@link EffectOutcome} describing any resource changes triggered by
     *         the insertion, or an empty outcome if no changes occurred
     */
    EffectOutcome onCharacterInsertion(CharacterType newCharacter);
}