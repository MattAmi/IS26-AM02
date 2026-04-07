package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.Era;
import it.polimi.ingsw.am02.model.Enumerations.EventType;

public class EventCard {

    //Attributi
    private final String cardID;
    private final Era era;
    private final EventType type;
    private final Boolean isFinal;
    private final int priority;
    private final EventEffect eventEffect;

    //Costruttore
    public EventCard(String cardID, Era era, EventType type, Boolean isFinal, int priority, EventEffect eventEffect) {
        this.cardID = cardID;
        this.era = era;
        this.type = type;
        this.isFinal = isFinal;
        this.priority = priority;
        this.eventEffect = eventEffect;
    }

    //Metodi
    public String getID() {
        return cardID;
    }

    public Era getEra() {
        return era;
    }

    public EventType getType() {
        return type;
    }

    public Boolean isFinal() {
        return isFinal;
    }

    public int getPriority() {
        return priority;
    }

    public void applyEventEffect(Game game) {
        eventEffect.applyEffect(game);
    }

    public static void attachEventObserver(EventObserver effect) {
    //TODO
    }

}