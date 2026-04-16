package it.polimi.ingsw.am02.server.model.listeners;

public interface GameObserverRegistry {
    void addObserver(GameObserver observer);
    void removeObserver(GameObserver observer);
}
