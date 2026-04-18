package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.server.model.listeners.GameObserver;

import java.util.List;

public interface ModelInterface {
    void addGameObserver(GameObserver observer);
    void removeGameObserver(GameObserver observer);
    void moveTotem(String nickname, char tileID);
    void resolveActions(String nickname, List<String> cardIDs);
}
