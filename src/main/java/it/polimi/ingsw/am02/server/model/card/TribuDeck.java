package it.polimi.ingsw.am02.server.model.card;

import it.polimi.ingsw.am02.server.model.GameRegistry;

import java.util.*;

/**
 * The main draw deck containing all character and non-building event cards,
 * stacked in era order (Era I on top, final events at the bottom).
 *
 * <p>Within each era, cards are shuffled using the provided random source.
 * Only cards with a {@code minPlayers} value {@code <=} the actual player count are included.
 */
public class TribuDeck {

    private final Stack<String> cards;

    /**
     * Builds and shuffles the deck for the given player count.
     *
     * @param numPlayers the number of players (used to filter cards by {@code minPlayers})
     * @param gameRandom the seeded random source used for shuffling
     */
    public TribuDeck(int numPlayers, Random gameRandom) {
        this.cards = new Stack<>();

        List<String> era1 = new ArrayList<>();
        List<String> era2 = new ArrayList<>();
        List<String> era3 = new ArrayList<>();
        List<String> eventiFinali = new ArrayList<>(); // Ora è una lista a parte, non un'Era

        GameRegistry registry = GameRegistry.getInstance();

        for (String id : registry.getAllCharactersIDs()) {
            CharacterCard card = registry.getCharacter(id);
            if (card != null && card.getMinPlayers() <= numPlayers) {
                switch(card.getEra()) {
                    case I -> era1.add(id);
                    case II -> era2.add(id);
                    case III -> era3.add(id); // I personaggi non sono mai finali
                }
            }
        }

        for (String id : registry.getAllEventsIDs()) {
            EventCard card = registry.getEvent(id);
            if (card != null) {
                if (card.isFinal()) {
                    eventiFinali.add(id);
                } else {
                    switch(card.getEra()) {
                        case I -> era1.add(id);
                        case II -> era2.add(id);
                        case III -> era3.add(id);
                    }
                }
            }
        }

        Collections.shuffle(eventiFinali, gameRandom);
        Collections.shuffle(era3, gameRandom);
        Collections.shuffle(era2, gameRandom);
        Collections.shuffle(era1, gameRandom);


        eventiFinali.forEach(this.cards::push);
        era3.forEach(this.cards::push);
        era2.forEach(this.cards::push);
        era1.forEach(this.cards::push);
    }

    /**
     * Draws and removes the top card from the deck.
     *
     * @return the card ID of the drawn card, or {@code null} if the deck is empty
     */
    public String draw() {
        if (!isEmpty()) {
            return this.cards.pop();
        }
        return null;
    }

    /** @return {@code true} if no cards remain in the deck */
    public boolean isEmpty() {
        return this.cards.isEmpty();
    }

    /** @return the number of cards remaining in the deck */
    public int getRemainingSize() {
        return this.cards.size();
    }

}