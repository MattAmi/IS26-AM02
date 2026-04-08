package it.polimi.ingsw.am02.model;

import java.util.*;

public class TribuDeck {

    private final Stack<String> cards;

    public TribuDeck(int numPlayers) {
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

        Collections.shuffle(eventiFinali);
        Collections.shuffle(era3);
        Collections.shuffle(era2);
        Collections.shuffle(era1);


        eventiFinali.forEach(this.cards::push);
        era3.forEach(this.cards::push);
        era2.forEach(this.cards::push);
        era1.forEach(this.cards::push);
    }

    public String draw() {
        if (!isEmpty()) {
            return this.cards.pop();
        }
        return null;
    }

    public boolean isEmpty() {
        return this.cards.isEmpty();
    }

    public int getRemainingSize() {
        return this.cards.size();
    }

}