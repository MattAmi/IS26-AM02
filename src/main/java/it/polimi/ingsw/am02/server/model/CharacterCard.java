package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.Era;

/**
 * Represents an immutable character card.
 * Each card belongs to a specific {@link Era}, has a {@link CharacterType},
 * and carries a {@link CharacterEffect} that is applied immediately when the card is drawn.
 */
public class CharacterCard {

    private final String cardID;
    private final Era era;
    private final CharacterType type;
    private final int minPlayers;
    private final CharacterEffect characterEffect;

    /**
     * @param cardID          unique identifier for this card (e.g. {@code "C_001"})
     * @param era             the game era this card belongs to
     * @param type            the character archetype (HUNTER, BUILDER, etc.)
     * @param minPlayers      minimum number of players required for this card to be included in the deck
     * @param characterEffect the effect applied when this card enters a tribe
     */
    public CharacterCard(String cardID, Era era, CharacterType type, int minPlayers, CharacterEffect characterEffect) {
        this.cardID = cardID;
        this.era = era;
        this.type = type;
        this.minPlayers = minPlayers;
        this.characterEffect = characterEffect;
    }

    /** @return the unique card identifier */
    public String getID() {
        return cardID;
    }

    /** @return the era this card belongs to */
    public Era getEra() {
        return era;
    }

    /** @return the character type (archetype) of this card */
    public CharacterType getType() {
        return type;
    }

    /** @return the minimum player count required for this card to appear in the game */
    public int getMinPlayers() {
        return minPlayers;
    }

    /**
     * Applies this card's character effect to the given player's tribe.
     *
     * @param player the player who just acquired this card
     * @return an {@link EffectOutcome} describing all resource changes produced by the effect
     */
    public EffectOutcome applyCharacterEffect(Player player) {
        return characterEffect.applyEffect(player);
    }

}
