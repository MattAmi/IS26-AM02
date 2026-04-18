package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.server.model.listeners.GameObserver;

public interface ModelInterface {
    void addGameObserver(GameObserver observer);
    void removeGameObserver(GameObserver observer);
}
