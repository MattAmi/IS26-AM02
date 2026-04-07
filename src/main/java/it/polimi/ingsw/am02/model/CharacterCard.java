package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.Era;

public class CharacterCard {
    //Attributi
    private final String cardID;
    private final Era era;
    private final CharacterType type;
    private final int minPlayers;
    private final CharacterEffect characterEffect;

    //Costruttore
    public CharacterCard(String cardID, Era era, CharacterType type, int minPlayers, CharacterEffect characterEffect) {
        this.cardID = cardID;
        this.era = era;
        this.type = type;
        this.minPlayers = minPlayers;
        this.characterEffect = characterEffect;
    }

    //Metodi
    public String getCardID() {
        return cardID;
    }

    public Era getEra() {
        return era;
    }

    public CharacterType getType() {
        return type;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void applyCharacterEffect(Tribu tribu) {
        characterEffect.applyEffect(tribu);
    }

}
