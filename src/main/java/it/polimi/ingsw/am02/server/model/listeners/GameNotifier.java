package it.polimi.ingsw.am02.server.model.listeners;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GameNotifier implements GameEventEmitter, GameObserverRegistry {

    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();

    @Override
    public void addObserver(GameObserver o) { observers.add(o); }

    @Override
    public void removeObserver(GameObserver o) { observers.remove(o); }
}
